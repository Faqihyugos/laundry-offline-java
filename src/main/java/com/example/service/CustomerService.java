package com.example.service;

import com.example.domain.Customer;
import com.example.repository.CustomerRepository;
import com.example.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public CustomerService(CustomerRepository customerRepository, OrderRepository orderRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    public List<Customer> findAll() {
        return customerRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Customer> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        return customerRepository.search(query.trim());
    }

    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    public Optional<Customer> findByPhone(String phone) {
        return customerRepository.findByPhone(phone);
    }

    public Customer createCustomer(String name, String phone, String address) {
        validateCustomerInput(name, phone);

        String cleanPhone = cleanPhoneNumber(phone);
        if (customerRepository.existsByPhone(cleanPhone)) {
            throw new IllegalArgumentException("Nomor telepon sudah terdaftar untuk pelanggan lain.");
        }

        Customer customer = new Customer(name.trim(), cleanPhone, address != null ? address.trim() : "");
        return customerRepository.save(customer);
    }

    public Customer updateCustomer(Long id, String name, String phone, String address) {
        validateCustomerInput(name, phone);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pelanggan tidak ditemukan"));

        String cleanPhone = cleanPhoneNumber(phone);
        customerRepository.findByPhone(cleanPhone).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Nomor telepon sudah terdaftar untuk pelanggan lain.");
            }
        });

        customer.setName(name.trim());
        customer.setPhone(cleanPhone);
        customer.setAddress(address != null ? address.trim() : "");
        return customerRepository.save(customer);
    }

    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pelanggan tidak ditemukan"));

        if (orderRepository.existsByCustomerId(id)) {
            throw new IllegalStateException("Pelanggan dengan riwayat transaksi tidak dapat dihapus.");
        }

        customerRepository.delete(customer);
    }

    private void validateCustomerInput(String name, String phone) {
        if (name == null || name.trim().length() < 2) {
            throw new IllegalArgumentException("Nama pelanggan wajib diisi minimal 2 karakter.");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Nomor telepon pelanggan wajib diisi.");
        }
        String clean = cleanPhoneNumber(phone);
        if (!clean.matches("^\\d{8,15}$")) {
            throw new IllegalArgumentException("Nomor telepon harus berupa angka dengan panjang 8-15 digit.");
        }
    }

    public static String cleanPhoneNumber(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[^0-9]", "");
    }

    public static String formatWhatsAppNumber(String phone) {
        String clean = cleanPhoneNumber(phone);
        if (clean.startsWith("0")) {
            return "62" + clean.substring(1);
        } else if (clean.startsWith("62")) {
            return clean;
        } else {
            return "62" + clean;
        }
    }

    public static String buildWhatsAppUrl(String phone, String message) {
        String waNumber = formatWhatsAppNumber(phone);
        if (message != null && !message.isEmpty()) {
            String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
            return "https://wa.me/" + waNumber + "?text=" + encoded;
        }
        return "https://wa.me/" + waNumber;
    }
}
