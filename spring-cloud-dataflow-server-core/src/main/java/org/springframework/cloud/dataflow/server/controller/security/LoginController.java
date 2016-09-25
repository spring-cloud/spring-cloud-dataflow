package org.springframework.cloud.dataflow.server.controller.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.server.controller.security.support.AuthenticationRequest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller responsible for handling web-logins as well as basic redirects for
 * for the admin-ui.
 *
 * @author Gunnar Hillert
 */
@Controller
public class LoginController {

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	ApplicationContext applicationContext;

	@RequestMapping(value = "/authenticate", method = { RequestMethod.POST })
	@ResponseBody
	public String authorize(
			@RequestBody AuthenticationRequest authenticationRequest,
			HttpServletRequest request) {

		final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
				authenticationRequest.getUsername(), authenticationRequest.getPassword());
		final Authentication authentication = this.authenticationManager.authenticate(token);
		SecurityContextHolder.getContext().setAuthentication(authentication);
		final HttpSession session = request.getSession(true);
		session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
				SecurityContextHolder.getContext());

		return session.getId();
	}

	@RequestMapping("/admin-ui/")
	String index() {
		return "forward:/admin-ui/index.html";
	}

	@RequestMapping("/admin-ui")
	String indexWithoutTrailingSlash() {
		return "redirect:/admin-ui/";
	}
}
