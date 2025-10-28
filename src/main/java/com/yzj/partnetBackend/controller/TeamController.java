package com.yzj.partnetBackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yzj.partnetBackend.common.BaseResponse;
import com.yzj.partnetBackend.common.DeleteRequest;
import com.yzj.partnetBackend.common.ErrorCode;
import com.yzj.partnetBackend.common.ResultUtils;
import com.yzj.partnetBackend.exception.BusinessException;
import com.yzj.partnetBackend.model.Team;
import com.yzj.partnetBackend.model.User;
import com.yzj.partnetBackend.model.UserTeam;
import com.yzj.partnetBackend.model.dto.TeamQuery;
import com.yzj.partnetBackend.model.request.*;
import com.yzj.partnetBackend.model.vo.TeamUserVo;
import com.yzj.partnetBackend.service.TeamService;
import com.yzj.partnetBackend.service.UserService;
import com.yzj.partnetBackend.service.UserTeamService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/team")
@CrossOrigin(origins = {"http://localhost:3000/",
        "http://localhost:5173/",
        "http://partnet-frontend.user-center-yzj.top",
        "https://partnet-frontend.user-center-yzj.top"},
        allowCredentials = "true")
@Slf4j
public class TeamController {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;

    /**
     * 业务流程：
     * 1、对传来的信息进行判空
     * 2、查询当前请求发送者的信息（request）
     * 3、执行核心业务（调用服务层）
     * 4、返回结果
     */

    /**
     * 新增队伍
     * @param teamAddRequest
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request){
        //@RequestBody表示json参数接收
        //1、判空
        if(teamAddRequest==null){
            throw new BusinessException(ErrorCode.NULL_ERROR);//请求参数为空
        }
        //2、判断是否登录
        User logininUser=userService.getLogininUser(request);
        //3、校验更多消息
        Team team = new Team();
        //传统写法是：B.setXX(A.getXX()),适用于只有几个特别的参数需要赋值
        //提取前者的属性值，将属性值赋值给后者
        //BeanUtils.copyProperties(team,teamAddRequest);
        BeanUtils.copyProperties(teamAddRequest,team);

        long teamId=teamService.addTeam(team,logininUser);
        return ResultUtils.success(teamId);
    }

    /**
     * 队长解散队伍（通用删除请求体）
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
        if(deleteRequest==null || deleteRequest.getId()<=0){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        long id=deleteRequest.getId();
        User loginUser=userService.getLogininUser(request);
        boolean result=teamService.deleteTeam(id,loginUser);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 修改队伍消息
     * @param teamUpdateRequest
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request){
        if (teamUpdateRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser=userService.getLogininUser(request);
        boolean result = teamService.updateTeam(teamUpdateRequest,loginUser);
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 跟据id查询某个队伍
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(long id){
        if (id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(team);
    }

    //------------------------------------------------------------------------------------------------------------------

//    /**
//     * 查询队伍列表
//     * @param teamQuery
//     * @return
//     */
//    @GetMapping("/list")
//    public BaseResponse<List<TeamUserVo>> listTeams(TeamQuery teamQuery,HttpServletRequest request){
//        if(teamQuery==null){
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        boolean isAdmin=userService.isAdmin(request);
//        //查询队伍列表
//        List<TeamUserVo> teamList=teamService.listTeams(teamQuery,isAdmin);
//        return ResultUtils.success(teamList);
//    }

    /**
     * 查询队伍列表（主要是查询当前登录用户加入了哪些队伍）
     * 保证仅加入队伍和创建队伍的人能看到队伍操作按钮
     * @param teamQuery
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<List<TeamUserVo>> listTeams(TeamQuery teamQuery,HttpServletRequest request){
        if(teamQuery==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean isAdmin=userService.isAdmin(request);
        //1、查询队伍列表
        List<TeamUserVo> teamList=teamService.listTeams(teamQuery,isAdmin);
        final List<Long> teamIdList=teamList.stream().map(TeamUserVo::getId).collect(Collectors.toList());

        //2、判断当前用户是否已加入队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper=new QueryWrapper<>();
        try{
            //取出当前登录用户id，去用户关系表查出是否加入过当前teamIdList的队伍
            User loginUser=userService.getLogininUser(request);
            userTeamQueryWrapper.eq("userId",loginUser);
            userTeamQueryWrapper.in("teamId",teamIdList);
            List<UserTeam> userTeamList=userTeamService.list(userTeamQueryWrapper);

            //当前用户已加入的队伍id的集合（去重？需要吗？）
            Set<Long> hasJoinTeamIdSet=userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
            teamList.forEach(team->{
                boolean hasJoin=hasJoinTeamIdSet.contains(team.getId());
                team.setHasJoin(hasJoin);
            });

        }catch (Exception e){

        }

        //3、查询已加入该队伍的人数（这里语法没看懂）
        QueryWrapper<UserTeam> userTeamJoinQueryWrapper=new QueryWrapper<>();
        userTeamJoinQueryWrapper.in("teamId",teamIdList);
        List<UserTeam> userTeamList=userTeamService.list(userTeamJoinQueryWrapper);
        //然后跟据队伍id，将查出来的数据分类
        Map<Long,List<UserTeam>> teamIdUserTeamList=
                userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        //将每个队伍的人数重新存回teamList里
        teamList.forEach(team->team.setHasJoinNum
                (teamIdUserTeamList.getOrDefault(team.getId(),new ArrayList<>()).size()));

        return ResultUtils.success(teamList);
    }

    /**
     * 分页查询队伍列表
     * @param teamQuery
     * @return
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listPageTeams(TeamQuery teamQuery){
        if(teamQuery==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team=new Team();
        //传统写法是：B.setXX(A.getXX()),适用于只有几个特别的参数需要赋值
        BeanUtils.copyProperties(teamQuery,team);//BeanUtils.copyProperties(赋值目标对象，模板源对象);

        Page<Team> page=new Page<>(teamQuery.getPageNum(),teamQuery.getPageSize());
        QueryWrapper<Team> queryWrapper=new QueryWrapper<>(team);
        Page<Team> resultPage=teamService.page(page,queryWrapper);
        return ResultUtils.success(resultPage);
    }

    /**
     * 获取当前用户创建过的队伍列表
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVo>> listMyCreateTeams(TeamQuery teamQuery,HttpServletRequest request){
        if(teamQuery==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User logininUser=userService.getLogininUser(request);
        boolean isAdmin=userService.isAdmin(request);
        teamQuery.setUserId(logininUser.getId());//区别就在于这里加入的userId，否则就是查全部队伍
        List<TeamUserVo> teamList=teamService.listTeams(teamQuery,isAdmin);
        return ResultUtils.success(teamList);
    }

    /**
     * 获取当前用户加入过的队伍列表
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVo>> listMyJoinTeams(TeamQuery teamQuery,HttpServletRequest request){
        if(teamQuery==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User logininUser=userService.getLogininUser(request);
        //查出与当前用户有联系的队伍，存在list里
        QueryWrapper<UserTeam> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("userId",logininUser.getId());
        List<UserTeam> userTeamList=userTeamService.list(queryWrapper);

        //取出不重复的队伍id，如：
        /*
        * teamId userId
        * 1,2
        * 1,3
        * 2,3
        * result
        * 1=> 2,3
        * 2=> 3
        * */
        //Stream 流提供了一个 collect() 方法，可以收集流中的数据到【集合】或者【数组】中去
        //Collectors.groupingBy配合Stream流使用，可以对集合中一个或多个属性进行分组，分组后还可以做聚合运算
        //UserTeam::getTeamId就是UserTeam里的teamId属性，即按照队伍id进行分组
        Map<Long,List<UserTeam>> listMap=userTeamList.stream()
                .collect(Collectors.groupingBy(UserTeam::getTeamId));
        //keySet 方法返回 Map 中所有 key 的集合，这里相当于将所有队伍id去重后放入集合里
        ArrayList<Long> idList=new ArrayList<>(listMap.keySet());

        //将当前的队伍id集合放进teamQuery
        teamQuery.setIdList(idList);
        List<TeamUserVo> teamList=teamService.listTeams(teamQuery,true);
        return ResultUtils.success(teamList);
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * 加入队伍
     * @param teamJoinRequest
     * @param request
     * @return
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request){
        if(teamJoinRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser=userService.getLogininUser(request);
        boolean result=teamService.joinTeam(teamJoinRequest,loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 用户退出队伍
     * @param teamQuitRequest
     * @param request
     * @return
     */
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request){
        if(teamQuitRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser=userService.getLogininUser(request);
        boolean result=teamService.quitTeam(teamQuitRequest,loginUser);
        return ResultUtils.success(result);
    }

}
