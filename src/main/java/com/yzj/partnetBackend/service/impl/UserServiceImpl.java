package com.yzj.partnetBackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yzj.partnetBackend.common.ErrorCode;
import com.yzj.partnetBackend.exception.BusinessException;
import com.yzj.partnetBackend.model.User;
import com.yzj.partnetBackend.service.UserService;
import com.yzj.partnetBackend.mapper.UserMapper;
import com.yzj.partnetBackend.utlis.AlgorithmUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.yzj.partnetBackend.constant.UserConstant.ADMIN_ROLE;
import static com.yzj.partnetBackend.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author 杨钲键
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-02-19 10:23:14
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Resource
    private UserMapper userMapper;

    //这个成员用来混淆密码的
    private static final String SALT="yzj";

//    class Solution {
//        public int maxProfit(int[] prices) {
//            int ans=0;
//
//            for(int i=1;i<prices.length;i++){
//                // 找到所有的正差值，全部算为利润
//                ans+=Math.max(prices[i]-prices[i-1],0);
//            }
//
//            return ans;
//        }
//    }

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword,String planetCode) {

        //1、参数校验
        //检查用户名、密码和确认密码是否为空，若为空则返回 -1，使用一个库来校验
        if(StringUtils.isAnyBlank(userAccount,userPassword,checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码长度过短");
        }
        if(planetCode.length()>5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"星球编号过长");
        }

        // 账户不能包含特殊字符
        String validateRegExp = "\\pP|\\pS|\\s+";
        Matcher matcher = Pattern.compile(validateRegExp).matcher(userAccount);
        if (matcher.find()) {
            return -1;
        }

        // 校验密码是否相同
        if (!userPassword.equals(checkPassword)) {
            return -1;
        }

        //校验账户是否重复
        LambdaQueryWrapper<User> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(User::getUserAccount,userAccount);
        Long l = userMapper.selectCount(lambdaQueryWrapper);
        if(l>0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号重复");
        }

        //星球编号不能重复
        LambdaQueryWrapper<User> lambdaQueryWrapper1=new LambdaQueryWrapper<>();
        lambdaQueryWrapper1.eq(User::getPlanetCode,planetCode);
        Long l1 = userMapper.selectCount(lambdaQueryWrapper1);
        if(l1>0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"星球账号重复");
        }

        //2、密码加密
        String s = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        //3、插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(s);
        user.setPlanetCode(planetCode);
        boolean saveResult = this.save(user);
        if(!saveResult){
            return -1;
        }

        return user.getId();
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1、参数校验
        //检查用户名、密码和确认密码是否为空，若为空则返回 -1，使用一个库来校验
        if(StringUtils.isAnyBlank(userAccount,userPassword)){
            return null;
        }
        if (userAccount.length() < 4) {
            return null;
        }
        if (userPassword.length() < 8 ) {
            return null;
        }

        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return null;
        }

        //2、密码加密
        String s = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        //校验账户和密码是否正确
        LambdaQueryWrapper<User> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        //注意左边对应的是数据库的字段，右边是等于的值
        lambdaQueryWrapper.eq(User::getUserAccount,userAccount);
        lambdaQueryWrapper.eq(User::getUserPassword,s);
        User user = userMapper.selectOne(lambdaQueryWrapper);
        if(user==null){
            //这里只输出日志
            log.info("user login failed,userAccount cannot match userPassword");
            //不必告诉前端到底是用户不存在还是密码错误，有一定安全性
            return null;
        }

        //3、用户脱敏，将密码去掉再返回
        User safetyUser = getSafetyUser(user);

        //4、记录用户的登录状态
        request.getSession().setAttribute(USER_LOGIN_STATE,safetyUser);

        return safetyUser;
    }

    /**
     * 用户脱敏
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser){
        if(originUser==null){
            return null;
        }

        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setProfile(originUser.getProfile());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setUpdateTime(originUser.getUpdateTime());
        safetyUser.setTags(originUser.getTags());

        return safetyUser;
    }

    @Override
    public int userLogout(HttpServletRequest request) {
        //移除用户登录状态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     *   根据标签搜索用户。
     * @param tagNameList  用户要搜索的标签
     * @return
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagNameList){

        //防止不加标签能把所有用户搜索出来，不安全
        if (CollectionUtils.isEmpty(tagNameList)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//        //方法一：sql查询
//        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
//        //拼接tag
//        // like '%Java%' and like '%Python%'
//        for (String tagList : tagNameList) {
//            lambdaQueryWrapper = lambdaQueryWrapper.like(User::getTags, tagList);
//        }
//        List<User> userList = userMapper.selectList(lambdaQueryWrapper);
//        return  userList.stream().map(this::getSafetyUser).collect(Collectors.toList());

        //方法二：内存查询
        //1、先查询所有用户
        QueryWrapper<User> QueryWrapper=new QueryWrapper<>();
        List<User> userList=userMapper.selectList(QueryWrapper);
        Gson gson=new Gson();
        //2、判断内存中是否包含要求的标签
        //反序列化：把json转为java对象
        return userList.stream().filter(user -> {
            String tagstr = user.getTags();
            if (StringUtils.isBlank(tagstr)){
                return false;
            }
            Set<String> tempTagNameSet =  gson.fromJson(tagstr,new TypeToken<Set<String>>(){}.getType());
            for (String tagName : tagNameList){
                if (!tempTagNameSet.contains(tagName)){
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());

        //map(this::getSafetyUser)：对每个用户对象脱敏（如移除密码字段）
        //collect(Collectors.toList())：收集结果到列表
    }

    /**
     * 获取当前用户信息
     * @param request
     * @return
     */
    @Override
    public User getLogininUser(HttpServletRequest request) {
        //判空
        if(request==null){
            return null;
        }
        //查看是否登录
        Object attribute = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser=(User) attribute;
        if(currentUser==null){
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }

        return currentUser;
    }

    /**
     * 修改用户信息
     * @param user 这个是前端直接传来的需要被修改的用户信息(已经在里面改好了信息)
     * @param loginUser 这个是前端传来的当前登录中的用户信息
     * @return
     */
    @Override
    public int userUpdate(User user, User loginUser) {
        // 现判断是否存在，免得浪费资源
        Long userId=user.getId();
        if(userId<=0){
            throw new BusinessException(ErrorCode.NO_LOGIN);//未登录
        }

        //如果是管理员，可以修改任意用户的信息
        //或者如果是普通用户，则只允许修改自己的信息
        //这里取反，方便返回错误信息
        if(!isAdmin(loginUser)&&userId!= loginUser.getId()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //验证被修改用户是否存在
        User user2=userMapper.selectById(userId);
        if(user2==null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        return userMapper.updateById(user);
    }

    /**
     * 判断是否为管理员
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        //仅限管理员查看
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user=(User) userObj;
        return user!=null && user.getUserRole()==ADMIN_ROLE;
    }

    @Override
    public boolean isAdmin(User loginUser) {
        return loginUser!=null && loginUser.getUserRole()==ADMIN_ROLE;
    }

    /**
     * 推荐匹配用户（还不是很理解）
     * @param num
     * @param loginUser
     * @return
     */
    @Override
    public List<User> matchUsers(long num, User loginUser) {
        //先排除标签为空的用户，减少匹配时间
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNotNull("tags");
        queryWrapper.select("id","tags");//仅查询id和tags字段，减少数据传输量
        List<User> userList = this.list(queryWrapper);
        //List<User> userList = this.page(new Page<>(pageNum,pageSize),queryWrapper);

        //logininUser.getTags()：获取当前用户的标签（JSON 字符串格式）
        String tags = loginUser.getTags();
        Gson gson = new Gson();
        //将 JSON 字符串转换为 List<String>
        //gson.fromJson()实现从Json相关对象到Java实体的方法,提供两个参数，分别是json字符串以及需要转换对象的类型
        //TypeToken，它是gson提供的数据类型转换器，可以支持各种数据集合类型转换
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        // 用户列表的下表 => 相似度'
        //Pair是一种简单的数据结构，用于存储两个元素作为一对
        //存储用户及其与当前用户的相似度（编辑距离）
        List<Pair<User,Long>> list = new ArrayList<>();
        // 依次计算当前用户和所有用户的相似度
        for (int i = 0; i <userList.size(); i++) {
            User user = userList.get(i);
            String userTags = user.getTags();
            //无标签的跳过,同时跳过自己
            if (StringUtils.isBlank(userTags) || user.getId() == loginUser.getId()){
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            //计算分数
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            list.add(new Pair<>(user,distance));
        }
        //按编辑距离有小到大排序
        //sorted(): 按编辑距离升序排序（相似度降序）。
        //limit(num): 限制结果数量。
        //collect(Collectors.toList()): 将流转换为列表
        List<Pair<User, Long>> topUserPairList = list.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        //有顺序的userID列表
        List<Long> userListVo = topUserPairList.stream().map(pari -> pari.getKey().getId()).collect(Collectors.toList());

        //根据id查询user完整信息
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id",userListVo);
        Map<Long, List<User>> userIdUserListMap = this.list(userQueryWrapper).stream()
                .map(user -> getSafetyUser(user))
                .collect(Collectors.groupingBy(User::getId));//按用户ID分组，生成Map<Long, List<User>>

        // 因为上面查询打乱了顺序，这里根据上面有序的userID列表赋值
        List<User> finalUserList = new ArrayList<>();
        for (Long userId : userListVo){
            finalUserList.add(userIdUserListMap.get(userId).get(0));
        }
        return finalUserList;
    }


//    @Override
//    public List<User> matchUsers(long num, User logininUser) {
//        //List<User>：动态数组，存储用户列表
//        List<User> userList = this.list();
//        //logininUser.getTags()：获取当前用户的标签（JSON 字符串格式）
//        String tags = logininUser.getTags();
//        Gson gson = new Gson();
//        //将 JSON 字符串转换为 List<String>
//        //gson.fromJson()实现从Json相关对象到Java实体的方法,提供两个参数，分别是json字符串以及需要转换对象的类型
//        //TypeToken，它是gson提供的数据类型转换器，可以支持各种数据集合类型转换
//        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
//        }.getType());
//        System.out.println(tagList);
//        // 用户列表的下表 => 相似度
//        //SortedMap（实现类 TreeMap）：按键自然排序的映射表，此处 key 为用户索引，value 为相似度
//        SortedMap<Integer, Long> indexDistanceMap = new TreeMap<>();
//        for (int i = 0; i <userList.size(); i++) {
//            User user = userList.get(i);
//            String userTags = user.getTags();
//            //无标签的跳过
//            if (StringUtils.isBlank(userTags)){
//                continue;
//            }
//            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
//            }.getType());
//            //计算分数
//            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
//            indexDistanceMap.put(i,distance);
//            //list.add(new Pair<>(user,distance));
//        }
//        //下面这个是打印前num个的id和分数
//        List<User> userListVo = new ArrayList<>();
//        int i = 0;
//        //entrySet 方法返回 Map 中所有键值对的集合。每一个元素都是一个 Map.Entry 对象，其中包含一个 key 和一个 value
//        for (Map.Entry<Integer,Long> entry : indexDistanceMap.entrySet()){
//            //超过当前需要查找的人数（num），就结束循环
//            if (i > num){
//                break;
//            }
//            User user = userList.get(entry.getKey());
//            System.out.println(user.getId() + ":" + entry.getKey() + ":" + entry.getValue());
//            userListVo.add(user);
//            i++;
//        }
//        return userListVo;
//    }

}




