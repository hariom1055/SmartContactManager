package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.persistence.criteria.Path;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ContactRepository contactRepository;
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	
	
	// Common data for all requests it will work 
	//method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String username = principal.getName();
		User user= userRepository.getUserByUserName(username);
		System.out.println("Username : "+username);
		System.out.println("User : "+user);
		
		model.addAttribute("user",user);
	}
	
	
	
	
	//Dashboard home
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		model.addAttribute("title","User Dashboard");
		return "normal/user_dashboard";
	}
	
	
	
	
	//open add form handler
	@GetMapping("/add_contact")
	public String openAddContactForm(Model model) {
		
		model.addAttribute("title","Add Contact");
		model.addAttribute("contact",new Contact());
		return "normal/add_contact_form";
	}

	
	
	//processing add contact form
	@PostMapping("/process-contact")
	public String processContact(
			@ModelAttribute Contact contact,
			@RequestParam("profileImage") MultipartFile file,
			Principal principal,
			HttpSession session) {
		
		try {
			String name = principal.getName();
			User user= this.userRepository.getUserByUserName(name);
			//processing and uploading file..
			if(file.isEmpty()) {
				contact.setImage("contact.png"); //this is a case when image is not choosen so we have given default image
			}
			else {
				//file to folder and update name to contact
				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/img").getFile();
				
				java.nio.file.Path path= Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("Image is Uploaded");
				
			}
			
			user.getContacts().add(contact);
			contact.setUser(user);
			
			this.userRepository.save(user);
			System.out.println("DATA "+contact);
			System.out.println("Aded to database");
			//message success..........
			//session.setAttribute("message", new Message("your contact is added ...ad more ","success"));	
			
		}
		catch (Exception e) {
			System.out.println("ERROR "+e.getMessage());
			e.printStackTrace();
			//message error..........
			//session.setAttribute("message", new Message("Something went wrong !! Try again","danger"));
			
			
			return "redirect:/user/add_contact?changeA=YSomething went wrong ! Try again";
		}

		//return "normal/add_contact_form";
		
		return "redirect:/user/add_contact?changeB=Your contact is added! you can add more";
	}

	
	
	
	//Show Contact Handler
	//per page we want 5 contact for pagination
	//current page = 0
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model m,Principal principal) {
		
		String username = principal.getName();
		User user = this.userRepository.getUserByUserName(username);
		
		//by changing this value 5 we can change no of contacts in pagination one page
		Pageable pageable = PageRequest.of(page, 5);  //has current page, contact per page
		Page<Contact> contacts = this.contactRepository.findContactByUser(user.getId(),pageable);
		
		m.addAttribute("title","Show Contacts");
		m.addAttribute("contacts",contacts);
		m.addAttribute("currentPage",page);
		m.addAttribute("totalPages",contacts.getTotalPages());
		
		return "normal/show_contacts";
	}

	
	
	//showing particular contact details
	@GetMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact= contactOptional.get();
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		if(user.getId()==contact.getUser().getId()) {
			model.addAttribute("contact",contact);
			model.addAttribute("title",contact.getName());
		}
		return "normal/contact_details";
	}

	
	
	//Delete Contact Handler
		//here contact is not deleted 
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId,Model model,HttpSession session,Principal principal) {
		
		Contact contact= this.contactRepository.findById(cId).get();
		//check for bug mean 
		//here contact is not deletes because at time of creating we have done cascade.ALL means it is link with user 
		//so we have to break link of contact and user. and we have done contact.setUser(null) this
		//but In actually it is only unlinked or removed from current user show page but not permanently deletes from database.
		//here we cannot change cascade all and this delete will work if we follow Service NTR architecture mean "contactRepository.delete(contact)" this operation should be in service  class with transaction annotation then it will work
		//but here we haven't used this architecture
		
		// orphanRemoval = true in user
		// contact is the child entity of User if contact is unlinked from user then contact will be removed.
		// Whether to apply the remove operation to entities that have been removed from the relationship and to cascade the remove operation tothose entities.
		
		User user= this.userRepository.getUserByUserName(principal.getName());
		user.getContacts().remove(contact);    //we get list of all contact then it will remove our contact("contact") from that list.
											   //here for doing this we need object matchinh
		this.userRepository.save(user);
		
		//contact.setUser(null);
		//this.contactRepository.delete(contact);
		
//		session.setAttribute("message",new Message("Successfully Deleted", "success"));
		
		return "redirect:/user/show-contacts/0";
	}

	
	
	
	//open Update Form Handler
	@PostMapping("/update-contact/{cid}")
	public String updateform(@PathVariable("cid") Integer cid ,Model model) {
		
		Contact contact = this.contactRepository.findById(cid).get();
		
		model.addAttribute("contact",contact);  
		model.addAttribute("title","Update Contact");
		return "normal/update_form";
	}
	
	
	
	
	//update contact handler
	@PostMapping("/process-update")
	public String updatehandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file, Model model, Principal principal) {
		
		try{
			Contact oldContactDetail = this.contactRepository.findById(contact.getcId()).get();
			if(!file.isEmpty()) {
				//file work , rewrite file
				//delete photo
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1= new File(deleteFile,oldContactDetail.getImage());
				file1.delete();
				
				//update photo
				File saveFile = new ClassPathResource("static/img").getFile();
				java.nio.file.Path path= Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
			}
			else {
				//file empty means -> file not changed
				contact.setImage(oldContactDetail.getImage());
			}
			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);	
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	
	
	
	//Profile Handler 
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		
		model.addAttribute("title","Profile Page");
		return "normal/profile";
	}
	
	
	
	
	//Open setting handler
	@GetMapping("/settings")
	public String openSettings() {
		
		return "normal/settings";
	}
	
	//Change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword, @RequestParam("newPassword") String newPassword,Principal principal) {
		
		System.out.println("Old Password : "+oldPassword);
		System.out.println("New Password : "+newPassword);
		
		String username = principal.getName();
		User currentUser = this.userRepository.getUserByUserName(username);
		//checking old password with user password
		if(this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())) {
			//change password
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			//now session wali picture
			
		}
		else {
			//error
			return "redirect:/user/settings?changeF=You have entered wrong old password";
		}
		
		return "redirect:/user/settings?changeT=Password has been changed successfully";
	}
	
	
}
