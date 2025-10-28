package com.yzj.partnetBackend.service;

import com.yzj.partnetBackend.model.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yzj.partnetBackend.model.User;
import com.yzj.partnetBackend.model.dto.TeamQuery;
import com.yzj.partnetBackend.model.request.TeamJoinRequest;
import com.yzj.partnetBackend.model.request.TeamQuitRequest;
import com.yzj.partnetBackend.model.request.TeamUpdateRequest;
import com.yzj.partnetBackend.model.vo.TeamUserVo;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author 杨钲键
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2025-03-26 10:36:04
*/
public interface TeamService extends IService<Team> {

    /**
     * 新增队伍
     * @param team
     * @param loginUser
     * @return
     */
    long addTeam(Team team, User loginUser);

    /**
     * 查询队伍列表
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    List<TeamUserVo> listTeams(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 修改队伍信息
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 申请加入队伍
     * @param teamJoinRequest
     * @param loginUser
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 用户退出队伍
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    /**
     * 队长解散队伍
     * @param id
     * @param loginUser
     * @return
     */
    boolean deleteTeam(long id, User loginUser);
}
