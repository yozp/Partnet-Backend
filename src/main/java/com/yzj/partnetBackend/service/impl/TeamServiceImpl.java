package com.yzj.partnetBackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yzj.partnetBackend.common.ErrorCode;
import com.yzj.partnetBackend.model.enums.TeamStatusEnum;
import com.yzj.partnetBackend.exception.BusinessException;
import com.yzj.partnetBackend.model.Team;
import com.yzj.partnetBackend.model.User;
import com.yzj.partnetBackend.model.UserTeam;
import com.yzj.partnetBackend.model.dto.TeamQuery;
import com.yzj.partnetBackend.model.request.TeamJoinRequest;
import com.yzj.partnetBackend.model.request.TeamQuitRequest;
import com.yzj.partnetBackend.model.request.TeamUpdateRequest;
import com.yzj.partnetBackend.model.vo.TeamUserVo;
import com.yzj.partnetBackend.model.vo.UserVo;
import com.yzj.partnetBackend.service.TeamService;
import com.yzj.partnetBackend.mapper.TeamMapper;
import com.yzj.partnetBackend.service.UserService;
import com.yzj.partnetBackend.service.UserTeamService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
* @author 杨钲键
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2025-03-26 10:36:04
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 大致业务流程
     * 1、第一个参数判空
     * 2、第二个参数判空
     * 3、对第一个参数的参数进行各种校验
     * 4、权限校验
     * 5、核心业务
     * 6、返回结果
     */

    @Transactional(rollbackFor = Exception.class)//@Transactional注解可以用来声明一个方法需要在事务的上下文中执行
    //rollbackFor属性是@Transactional注解的一个重要属性，它用于指定哪些异常类型会触发事务回滚
    //要么数据操作都成功，要么都失败
    @Override
    public long addTeam(Team team, User loginUser) {
        //1. 请求参数是否为空？
        if (team == null){
            throw  new BusinessException(ErrorCode.NULL_ERROR);
        }
        //2. 是否登录，未登录不允许创建
        if (loginUser == null) {
            throw  new BusinessException(ErrorCode.NO_LOGIN);
        }
        final long userId=loginUser.getId();


        //3、校验信息
        //  a. 队伍人数 >= 1 且 <= 20
        //Optional.ofNullable(值1).orElse(值2)就是一个包装好的三元运算，避免空指针
        //值1不空取值1，值1空了取值2
        Integer maxNum= Optional.ofNullable(team.getMaxNum()).orElse(0);
        if(maxNum<1||maxNum>20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍人数不符合要求");
        }
        //  b. 队伍标题 <= 20
        String name = team.getName();
        //StringUtils的isBlank()方法可以一次性校验这三种情况，返回值都是true,否则为false
        //是否为 null,是否为 “”,是否为空字符串(引号中间有空格) ~~ 如：" "
        if(StringUtils.isBlank(name)||name.length()>20){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍标题不满足要求");
        }
        //  c. 描述 <= 512
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述过长");
        }
        //  d. status 是否公开（int）不传默认为 0（公开）
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不满足要求");
        }
        //  e. 如果 status 是加密状态，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        //先判断是否私密
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            //判断密码合法性
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码设置不正确");
            }
        }
        //  f. 超时时间 > 当前时间（意思是到了过期时间队伍就会删除）
        Date expireTime = team.getExpireTime();
        //Date().after():检查此日期是否在指定的date 之后
        if (new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间 > 当前时间");
        }
        //  g. 校验用户最多创建 5 个队伍
        // todo 有 bug，可能同时创建 100 个队伍(瞬间点击100下)
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        //查出该用户id所加过的队伍数量
        //这里的this是指TeamService的save方法，也就是这个当前类的本身
        long hasTeamNum = this.count(queryWrapper);
        if (hasTeamNum > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建 5 个队伍");
        }


        //4. 插入队伍信息 => 到队伍表
        team.setId(null);//因为会自增，所以先置空
        team.setUserId(userId);
        boolean result=this.save(team);
        Long teamId=team.getId();//取出刚刚插入的team
        if(!result||teamId==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"创建队伍失败");
        }

        //5. 插入用户 => 队伍关系到关系表
        UserTeam userTeam=new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result= userTeamService.save(userTeam);
        if(!result){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"创建队伍失败");
        }

        return teamId;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<TeamUserVo> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();

        //1、组合查询条件
        //进行一系列判空，非空则加入查询条件
        if(teamQuery!=null){
            //对于String判空（这里是非空）：StringUtils.isNotBlank(xx)
            //对于集合判空（这里是非空）：CollectionUtils.isNotEmpty(xx)
            //对于Integer、Long判空：xx != null && xx > 0

            Long id=teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            List<Long> idList = teamQuery.getIdList();
            if (CollectionUtils.isNotEmpty(idList)) {
                queryWrapper.in("id", idList);
            }
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            Integer maxNum = teamQuery.getMaxNum();
            // 查询最大人数相等的
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }
            Long userId = teamQuery.getUserId();
            // 根据创建人来查询
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }
            // 根据状态来查询（若是空值则默认公开）
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if (statusEnum == null) {
                statusEnum = TeamStatusEnum.PUBLIC;
            }
            //TODO 待优化
            //权限管理(查询私密队伍时，如果不是管理员，则不能查询，无权限)
            if (!isAdmin && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
            queryWrapper.eq("status", statusEnum.getValue());
        }

        //2、 不展示已过期的队伍
        // 相当于sql：expireTime is null or expireTime > now()
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        List<Team> teamList = this.list(queryWrapper);
        //如果查不到任何队伍，则创建一个
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }

        //3、返回查询结果信息
        List<TeamUserVo> teamUserVoList=new ArrayList<>();
        for(Team team:teamList){
            //取出该队伍的队长id
            Long userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            User user = userService.getById(userId);
            TeamUserVo teamUserVO = new TeamUserVo();
            BeanUtils.copyProperties(team, teamUserVO);
            // 脱敏用户信息/确保该用户还存在
            if (user != null) {
                UserVo userVO = new UserVo();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVoList.add(teamUserVO);
        }
        return teamUserVoList;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = teamUpdateRequest.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team oldTeam = this.getById(id);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        // 只有管理员或者队伍的创建者可以修改
        if (oldTeam.getUserId() != loginUser.getId() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        //判断当前修改后的队伍状态
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus());
        if (statusEnum.equals(TeamStatusEnum.SECRET)) {
            //私密状态必须设置密码
            if (StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密房间必须要设置密码");
            }
        }

        //返回修改结果
        Team updateTeam = new Team();
        //将TeamUpdateRequest类的属性重新赋值给Team类。再去修改信息
        BeanUtils.copyProperties(teamUpdateRequest, updateTeam);
        return this.updateById(updateTeam);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if (teamJoinRequest == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Long teamId = teamJoinRequest.getTeamId();
        if (teamId == null || teamId <= 0){
            throw  new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null){
            throw  new BusinessException(ErrorCode.NULL_ERROR,"队伍不存在");
        }
        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍已过期");
        }
        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"禁止加入私有队伍");
        }
        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)){
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码错误");
            }
        }
        //该用户已加入的队伍数量
        long userId = loginUser.getId();

        //下面的代码模板在PreCacheJob类里面
        // 只有一个线程能获取到锁
        RLock lock=redissonClient.getLock("yzj:join_team");
        try{
            while(true){
                //尝试获取锁，等待0秒（表示一天只抢一次，抢不到就走了），持有锁-1秒钟
                //这里运用了看门狗机制
                if(lock.tryLock(0,-1, TimeUnit.MILLISECONDS)){
                    //Thread.currentThread()可以获取当前线程的引用,getId();获取该线程的标识符
                    System.out.println("getLock: " + Thread.currentThread().getId());

                    QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId",userId);
                    long hasJoinNum = userTeamService.count(userTeamQueryWrapper);
                    if (hasJoinNum >= 5){
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,"最多创建和加入5个队伍");
                    }
                    //不能重复加入已加入的队伍
                    userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId",userId);
                    userTeamQueryWrapper.eq("teamId",teamId);
                    long hasUserJoinTeam = userTeamService.count(userTeamQueryWrapper);
                    if (hasUserJoinTeam > 0){
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户已加入该队伍");
                    }
                    //已加入队伍的人数
                    userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("teamId",teamId);
                    long teamHasJoinNum = userTeamService.count(userTeamQueryWrapper);
                    if (teamHasJoinNum >= team.getMaxNum()){
                        throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍已满");
                    }

                    //加入，修改队伍信息
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());

                    return userTeamService.save(userTeam);
                }
            }
        }catch (InterruptedException e){
            log.error("doCacheRecommendUser error", e);
            return false;
        }finally {
            // 是否是当前执行线程的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();//释放锁
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        if (teamQuitRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamQuitRequest.getTeamId();
        if (teamId == null || teamId <= 0){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"队伍不存在");
        }
        long userId = loginUser.getId();
        UserTeam queryUserTeam = new UserTeam();
        queryUserTeam.setUserId(userId);
        queryUserTeam.setTeamId(teamId);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>(queryUserTeam);
        long count = userTeamService.count(queryWrapper);
        if (count == 0 ){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"未加入队伍");
        }
        long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
        // 队伍只剩一人，解散
        if (teamHasJoinNum == 1) {
            // 删除队伍
            this.removeById(teamId);
        } else {
            // 队伍还剩至少两人
            // 是队长
            if (team.getUserId() == userId) {
                // 把队伍转移给最早加入的用户
                // 1. 查询已加入队伍的所有用户和加入时间
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.eq("teamId", teamId);
                userTeamQueryWrapper.last("order by id asc limit 2");
                List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
                if (CollectionUtils.isEmpty(userTeamList) || userTeamList.size() <= 1) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                UserTeam nextUserTeam = userTeamList.get(1);
                Long nextTeamLeaderId = nextUserTeam.getUserId();
                // 更新当前队伍的队长
                Team updateTeam = new Team();
                updateTeam.setId(teamId);
                updateTeam.setUserId(nextTeamLeaderId);
                boolean result = this.updateById(updateTeam);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍队长失败");
                }
            }
        }
        // 移除关系
        return userTeamService.remove(queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteTeam(long id, User loginUser) {
        // 校验队伍是否存在
        Team team =this.getById(id);//这里原本是Team team = getTeamById(id);但是报错了
        long teamId = team.getId();
        // 校验你是不是队伍的队长
        if (team.getUserId() != loginUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无访问权限");
        }
        // 移除所有加入队伍的关联信息
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        boolean result = userTeamService.remove(userTeamQueryWrapper);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍关联信息失败");
        }
        // 删除队伍
        return this.removeById(teamId);
    }

    /**
     * 获取某队伍当前人数
     * @param teamId
     * @return
     */
    private long countTeamUserByTeamId(long teamId) {
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        return userTeamService.count(userTeamQueryWrapper);
    }
}




