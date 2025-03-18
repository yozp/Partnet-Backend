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
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.util.List;
import java.util.Set;
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

}




