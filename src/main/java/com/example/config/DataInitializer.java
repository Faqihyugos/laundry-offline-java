package com.example.config;

import com.example.domain.*;
import com.example.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final LaundryServiceRepository laundryServiceRepository;
    private final AppSettingRepository appSettingRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           CustomerRepository customerRepository,
                           LaundryServiceRepository laundryServiceRepository,
                           AppSettingRepository appSettingRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.laundryServiceRepository = laundryServiceRepository;
        this.appSettingRepository = appSettingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initSettings();
        initUsers();
        initServices();
        initCustomers();
    }

    private void initSettings() {
        createSettingIfAbsent("laundry_name", "Laundry Bersih Wangi");
        createSettingIfAbsent("laundry_address", "Jl. Sudirman No. 123, Jakarta");
        createSettingIfAbsent("laundry_phone", "6281234567890");
        createSettingIfAbsent("invoice_prefix", "LBW");
        createSettingIfAbsent("printer_paper_size", "58mm");
    }

    private void createSettingIfAbsent(String key, String value) {
        if (!appSettingRepository.existsBySettingKey(key)) {
            appSettingRepository.save(new AppSetting(key, value));
        }
    }

    private void initUsers() {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(new User("admin", passwordEncoder.encode("admin123"), "Owner Toko", Role.OWNER));
        }
        if (!userRepository.existsByUsername("kasir")) {
            userRepository.save(new User("kasir", passwordEncoder.encode("kasir123"), "Kasir Utama", Role.CASHIER));
        }
    }

    private void initServices() {
        if (laundryServiceRepository.count() == 0) {
            laundryServiceRepository.save(new LaundryService("Cuci Kering Reguler", BigDecimal.valueOf(7000), ServiceUnit.KG, 48));
            laundryServiceRepository.save(new LaundryService("Cuci Setrika Reguler", BigDecimal.valueOf(10000), ServiceUnit.KG, 48));
            laundryServiceRepository.save(new LaundryService("Cuci Setrika Express", BigDecimal.valueOf(15000), ServiceUnit.KG, 6));
            laundryServiceRepository.save(new LaundryService("Setrika Saja", BigDecimal.valueOf(6000), ServiceUnit.KG, 24));
            laundryServiceRepository.save(new LaundryService("Bedcover Besar", BigDecimal.valueOf(35000), ServiceUnit.PCS, 48));
            laundryServiceRepository.save(new LaundryService("Selimut / Bedcover Kecil", BigDecimal.valueOf(25000), ServiceUnit.PCS, 48));
            laundryServiceRepository.save(new LaundryService("Sepatu Sneaker", BigDecimal.valueOf(30000), ServiceUnit.PCS, 72));
            laundryServiceRepository.save(new LaundryService("Jas / Blazer", BigDecimal.valueOf(40000), ServiceUnit.PCS, 48));
        }
    }

    private void initCustomers() {
        if (customerRepository.count() == 0) {
            customerRepository.save(new Customer("Budi Santoso", "081234567891", "Jl. Mawar No. 10, Jakarta"));
            customerRepository.save(new Customer("Siti Rahma", "081298765432", "Jl. Melati No. 5, Jakarta"));
            customerRepository.save(new Customer("Agus Pratama", "085612345678", "Jl. Kenanga No. 8, Jakarta"));
        }
    }
}
