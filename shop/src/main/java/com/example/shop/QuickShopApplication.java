package com.example.shop;

import com.example.shop.config.AppConstants;
import com.example.shop.repository.RoleRepo;
import com.example.shop.repository.UserRepo;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.modelmapper.ModelMapper;
import com.example.shop.entity.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

@EnableScheduling//Chạy theo giờ
@SpringBootApplication
@SecurityScheme(name = "E-commerce Application",scheme = "bearer",type = SecuritySchemeType.HTTP,in = SecuritySchemeIn.HEADER)
public class QuickShopApplication implements CommandLineRunner {
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private RoleRepo roleRepo;
	public static void main(String[] args) {
        SpringApplication.run(QuickShopApplication.class, args);
	}
    @Bean
    public ModelMapper modelMapper(){
        return new ModelMapper();
    }
    @Override
    public void run(String ...args)throws Exception{
        try{
            Role adminRole=new Role();
            adminRole.setId(AppConstants.ADMIN_ID);
            adminRole.setRoleName(AppConstants.POSITION_ADMIN);
            Role employeeRole=new Role();
            employeeRole.setId(AppConstants.EMPLOYEE);
            employeeRole.setRoleName(AppConstants.POSITION_STAFF);

            Role customerRole = new Role();
            customerRole.setId(AppConstants.USER_ID);
            customerRole.setRoleName(AppConstants.POSITION_CUSTOMER);
    
            Role managerRole = new Role();
            managerRole.setId(AppConstants.MANAGER); // ID tự choose, other ADMIN và EMPLOYEE
            managerRole.setRoleName(AppConstants.POSITION_MANAGER);
            List<Role>roles=List.of(adminRole,employeeRole,customerRole,managerRole);
            List<Role>saveRoles=roleRepo.saveAll(roles);

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
