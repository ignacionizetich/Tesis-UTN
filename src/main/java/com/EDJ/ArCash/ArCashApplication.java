package com.EDJ.ArCash;


import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Optional;
import java.util.Scanner;


/// ES POSIBLE BUILDEAR LA APP EN RAILWAY SIN DOCKER FILE. SOLO JAVA 17.
/// https://arcash.ddns.net/

/// si queremos testear algo, usemos este codigo y modifiquemoslo:
///
/// @Bean public CommandLineRunner commandLineRunner() {
///
/// 		metodo();
/// 		return null;
///    }

@EnableScheduling
@SpringBootApplication
public class ArCashApplication {

    private final AccountRepository accountRepository;

    private final  Scanner scanner = new Scanner(System.in);

    public ArCashApplication(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(ArCashApplication.class, args);




    }

    @Bean
    public CommandLineRunner commandLineRunner() {



        System.out.println("Ingrese un alias o cvu de cuenta para transferir: ");
        String aliascvu = scanner.nextLine();
        Optional<Account> optional = accountRepository.findByAccountNickname(aliascvu);

     if(optional.isPresent()){
         System.out.println("CUENTA ENCONTRADA: ");
         Account account = optional.get();
         System.out.println(optional.get().getAccountCvu());
         System.out.println(optional.get().getAccountNickname());
         System.out.println(account.getUser().mostrarInformacion());

     }else {

         Optional<Account> optionalCvu = accountRepository.findByAccountCvu(aliascvu);

         if(optionalCvu.isPresent()){
             System.out.println("CUENTA ENCONTRADA: ");
             Account account = optionalCvu.get();
             System.out.println(optionalCvu.get().getAccountCvu());
             System.out.println(optionalCvu.get().getAccountNickname());
             System.out.println(account.getUser().mostrarInformacion());
         }else {
             System.out.println("No existe ninguna cuenta con ese alias o cvu");
         }

     }

        return null;
    }
}
