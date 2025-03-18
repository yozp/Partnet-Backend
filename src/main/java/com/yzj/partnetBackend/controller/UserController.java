package com.yzj.partnetBackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yzj.partnetBackend.common.BaseResponse;
import com.yzj.partnetBackend.common.ErrorCode;
import com.yzj.partnetBackend.common.ResultUtils;
import com.yzj.partnetBackend.exception.BusinessException;
import com.yzj.partnetBackend.model.User;
import com.yzj.partnetBackend.model.request.UserLoginRequest;
import com.yzj.partnetBackend.model.request.UserRegisterRequest;
import com.yzj.partnetBackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static com.yzj.partnetBackend.constant.UserConstant.ADMIN_ROLE;
import static com.yzj.partnetBackend.constant.UserConstant.USER_LOGIN_STATE;

@RestController
@RequestMapping("/user")
//这个@CrossOrigin已经够用了，这个WebMvcConfig.java可以不需要，内容是没有错的
@CrossOrigin(origins = {"http://localhost:3000/"},allowCredentials = "true")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest){

        //验证整体不为空
        if(userRegisterRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String userAccount=userRegisterRequest.getUserAccount();
        String userPassword=userRegisterRequest.getUserPassword();
        String checkPassword=userRegisterRequest.getCheckPassword();
        String planetCode=userRegisterRequest.getPlanetCode();
        //验证局部不为空
        if(StringUtils.isAnyBlank(userAccount,userPassword,checkPassword,planetCode)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long result=userService.userRegister(userAccount, userPassword, checkPassword,planetCode);

        //return new BaseResponse<>(0,result,"ok");
        //这样太麻烦了，自定义一个包装类，然后设置一个快捷键
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){

        //请求参数错误
        if(userLoginRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String userAccount=userLoginRequest.getUserAccount();
        String userPassword=userLoginRequest.getUserPassword();
        //请求参数错误
        if(StringUtils.isAnyBlank(userAccount,userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User user=userService.userLogin(userAccount, userPassword, request);
        //return new BaseResponse<>(0,user,"ok");
        return ResultUtils.success(user);
    }

    /**
     * 查询用户(管理员权限)
     * @param username
     * @return
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username,HttpServletRequest request){
        if(!userService.isAdmin(request)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //将相似的用户名都查出来
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if(StringUtils.isNotBlank(username)){
            queryWrapper.like("username",username);
        }

        List<User> userList = userService.list(queryWrapper);

        //返回包装类
        List<User> list=userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    /**
     * 跟据标签tags查询用户
     * @param tagNameList
     * @return
     */
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList){
        //检查是否为空,非管理员也能查
        if(CollectionUtils.isEmpty(tagNameList)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        List<User> userList = userService.searchUsersByTags(tagNameList);

        //返回包装类
        return ResultUtils.success(userList);
    }


    /**
     * 删除用户
     * @param id
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id,HttpServletRequest request){
        if(!userService.isAdmin(request)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        if(id<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean result=userService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录状态、信息接口
     * @param request
     * @return
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request){
        //查看是否登录
        Object attribute = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser=(User) attribute;
        if(currentUser==null){
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }

        long userId=currentUser.getId();
        //TODO 校验用户是否合法
        User user=userService.getById(userId);
        //脱敏再返回
        User safetyUser=userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }

    /**
     * 用户注销（退出登录）
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request){
        if(request==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //移除用户登录态
        int result=userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 修改用户信息
     * @param user 这个是前端直接传来的需要被修改的用户信息(已经在里面改好了信息)
     * @param request 前端的请求信息（包含当前登录的用户信息）
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Integer> userUpdate(@RequestBody User user,HttpServletRequest request){
        //先验证参数是否为空
        if(user==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //鉴权
        User loginUser=userService.getLogininUser(request);
        //修改
        int result=userService.userUpdate(user,loginUser);

        return ResultUtils.success(result);
    }

//    //这个方法现在封装在server层
//    /**
//     * 检查是否为管理员
//     * @param request
//     * @return
//     */
//    private boolean isAdmin(HttpServletRequest request){
//        //仅限管理员查看
//        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
//        User user=(User) userObj;
//        return user!=null && user.getUserRole()==ADMIN_ROLE;
//    }

}
