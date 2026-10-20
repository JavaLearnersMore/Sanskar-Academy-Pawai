package com.sanskaracademy.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sanskaracademy.entity.Expense;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.DashboardService;
import com.sanskaracademy.service.ExpenseService;
import com.sanskaracademy.service.FinanceService;
import com.sanskaracademy.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

	private final UserService userService;
	private final UserRepository userRepository;
	private final FinanceService financeService;
	private final ExpenseService expenseService;

	private final DashboardService dashboardService;

	@GetMapping("/dashboard")
	public String dashboard(Model model) {

		//    	List<User> students = userService.findAllStudents();
		//        model.addAttribute("users", userService.getAllUsers());
		//        model.addAttribute("students", students);
		//        model.addAttribute("totalStudents", students.size());

		Map<String, Object> stats = dashboardService.getAdminDashboardStats();
		model.addAllAttributes(stats);
		return "admin/dashboard";
	}

	@PostMapping("/regenerate-secret/{username}")
	public String regenerateSecret(@PathVariable String username) {
		userService.regenerateSecretForUser(username);
		return "redirect:/admin/dashboard";
	}

	@GetMapping("/pending-users")
	public String pendingUsers(Model model) {
		model.addAttribute("users",
				userRepository.findByEmailVerifiedTrueAndApprovedByAdminFalse());
		return "admin/pending-users";
	}

	@PostMapping("/approve-user/{id}")
	public String approveUser(@PathVariable Long id) {

		User user = userRepository.findById(id).orElseThrow();
		user.setApprovedByAdmin(true);
		user.setEnabled(true);
		userRepository.save(user);

		return "redirect:/admin/pending-users";
	}


	@GetMapping("/students")
	public String students(Model model) {
		model.addAttribute("students", userService.findAllStudents());
		return "admin/students";
	}

	@GetMapping("/teachers")
	public String teachers(Model model) {
		model.addAttribute("teachers", userService.findAllTeachers());
		return "admin/teachers";
	}

	@GetMapping("/finance")
	public String finance(Model model) {
		Map<String, BigDecimal> finance = financeService.getFinanceSummary();
		model.addAttribute("income", finance.get("income"));
		model.addAttribute("expenses", finance.get("expenses"));
		model.addAttribute("profit", finance.get("profit"));
		return "admin/finance";
	}

	@GetMapping("/settings")
	public String settings(Model model) {	

		List<User> pendingUsers = userRepository.findPendingUsers();

		model.addAttribute("pendingUsers", pendingUsers);

		return "admin/settings";
	}
	
	@GetMapping("/expenses")
	public String expenses(Model model) {

	    model.addAttribute("expense", new Expense());
	    model.addAttribute("expenses", expenseService.getAllExpenses());

	    return "admin/expenses";
	}

	@PostMapping("/expenses")
	public String addExpense(@ModelAttribute Expense expense,
	                         Principal principal) {

	    User user = userRepository.findByUsername(principal.getName())
	            .orElseThrow();

	    expenseService.saveExpense(expense, user);

	    return "redirect:/admin/expenses";
	}

	@PostMapping("/delete-expense/{id}")
	public String deleteExpense(@PathVariable Long id) {

	    expenseService.deleteExpense(id);
	    return "redirect:/admin/expenses";
	}
}