package com.smart.controller;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.helper.Message;
import com.smart.service.EmailService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ForgotController {
	Random random = new Random(1000);
	
	@Autowired
	private EmailService emailService;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	//email id open handler
	@RequestMapping("/forgot")
	public String openEmailForm() {
		return "forgot_email_form";
	}
	
	@PostMapping("/send-otp")
	public String sendOTP(@RequestParam("email") String email,HttpSession session) {
		
		System.out.println(email);
		
		//generating otp of 4 digits
		int otp = random.nextInt(999999);
		System.out.println("OTP : "+otp);
		
		//write code for OTP send to email
		String subject ="OTP from Smart Contact Manager";
		String message=""
				+"<div class='border: 1px solid #e2e2e2; padding:20px'>"
				+"<h1>"
				+"OTP is : "
				+"<b>"+otp
				+"</b>"
				+"</h1>"
				+"</div>";
		String to=email;
		
		boolean flag=this.emailService.sendEmail(subject, message, to);
		if(flag) {
			session.setAttribute("email", email);
			session.setAttribute("myotp",otp);
			//return "verify_otp";
			return "redirect:/verify?change=We have sent OTP to your email";
		}
		else {
			//session.setAttribute("message", "Error in sending otp");
			return "forgot_email_form";
		}
			
	}
	
	@RequestMapping("/verify")
	public String veridy() {
		return "verify_otp";
	}
	
	//verify otp
	@PostMapping("/verify-otp")
	public String verfyOtp(@RequestParam("otp") int otp,HttpSession session) {
		
		int myOtp=(int) session.getAttribute("myotp");
		String email = (String) session.getAttribute("email");
		if(myOtp==otp) {
			//password change form
			System.out.println("you are inside");
			User user = this.userRepository.getUserByUserName(email);
			if(user==null) {
				//send error message
				//session.setAttribute("message", "User does not exist with this email !!");
				return "redirect:/forgot?changeF=User does not exist with this email !!";
				
			}else {
				//send change password form
				
			}
			
			
			return "password_change_form";
			
			
		}
		else {
			//session.setAttribute("message","You have entered wrong otp");
			return "redirect:/verify?changeW= Wrong OTP ! Enter correct one ";
		}
		
		
	}
	
	//change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newpassword") String newpassword,HttpSession session) {
		String email = (String) session.getAttribute("email");
		User user = this.userRepository.getUserByUserName(email);
		user.setPassword(this.bCryptPasswordEncoder.encode(newpassword));
		this.userRepository.save(user);
		
		return "redirect:/signin?change=password changed successfuly..";
	}
	
}
