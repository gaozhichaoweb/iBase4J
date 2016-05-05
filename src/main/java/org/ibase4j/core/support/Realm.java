package org.ibase4j.core.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.ibase4j.core.Constants;
import org.ibase4j.mybatis.generator.model.SysMenu;
import org.ibase4j.mybatis.generator.model.SysUser;
import org.ibase4j.service.sys.SysAuthorizeService;
import org.ibase4j.service.sys.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.pagehelper.PageInfo;


public class Realm extends AuthorizingRealm {
	@Autowired
	private SysAuthorizeService sysAuthorizeService;
	@Autowired
	private SysUserService sysUserService;

	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		String currentUserAccount = (String) super.getAvailablePrincipal(principals);
		List<String> roleList = new ArrayList<String>();
		List<String> permissionList = new ArrayList<String>();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("countSql", 0);
		params.put("usable", 1);
		params.put("account", currentUserAccount);
		PageInfo<SysUser> pageInfo = sysUserService.query(params);
		if (pageInfo.getSize() == 1) {
			SysUser user = pageInfo.getList().get(0);
			// 从数据库中获取当前登录用户的详细信息
			List<SysMenu> menus = sysAuthorizeService.getAuthorize(user.getId());
			if (null != menus) {
				for (SysMenu pmss : menus) {
					if (StringUtils.isNotBlank(pmss.getRequest())) {
						permissionList.add(pmss.getRequest());
					}
				}
			} else {
				throw new AuthorizationException();
			}
			// 为当前用户设置角色和权限
			SimpleAuthorizationInfo simpleAuthorInfo = new SimpleAuthorizationInfo();
			simpleAuthorInfo.addRoles(roleList);
			simpleAuthorInfo.addStringPermissions(permissionList);
			return simpleAuthorInfo;
		}
		return null;
	}

	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authcToken)
			throws AuthenticationException {
		UsernamePasswordToken token = (UsernamePasswordToken) authcToken;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("countSql", 0);
		params.put("usable", 1);
		params.put("account", token.getUsername());
		StringBuilder sb = new StringBuilder(100);
		for (int i = 0; i < token.getPassword().length; i++) {
			sb.append(token.getPassword()[i]);
		}
		params.put("password", sb.toString());
		PageInfo<SysUser> pageInfo = sysUserService.query(params);
		if (pageInfo.getSize() == 1) {
			SysUser user = pageInfo.getList().get(0);
			setSession(Constants.CURRENT_USER, user);
			AuthenticationInfo authcInfo = new SimpleAuthenticationInfo(user.getAccount(), user.getPassword(),
					user.getUserName());
			return authcInfo;
		} else {
			return null;
		}
	}

	/**
	 * 将一些数据放到ShiroSession中,以便于其它地方使用
	 * 
	 * @see 比如Controller,使用时直接用HttpSession.getAttribute(key)就可以取到
	 */
	private void setSession(Object key, Object value) {
		Subject currentUser = SecurityUtils.getSubject();
		if (null != currentUser) {
			Session session = currentUser.getSession();
			if (null != session) {
				session.setAttribute(key, value);
			}
		}
	}
}