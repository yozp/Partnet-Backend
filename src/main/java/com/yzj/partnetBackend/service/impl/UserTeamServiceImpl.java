package com.yzj.partnetBackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yzj.partnetBackend.model.UserTeam;
import com.yzj.partnetBackend.service.UserTeamService;
import com.yzj.partnetBackend.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
* @author 杨钲键
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2025-03-26 10:39:55
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

}




