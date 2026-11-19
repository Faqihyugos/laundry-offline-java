package com.example.service;

import com.example.domain.LaundryService;
import com.example.domain.ServiceUnit;
import com.example.repository.LaundryServiceRepository;
import com.example.repository.OrderItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ServicePackageService {

    private final LaundryServiceRepository serviceRepository;
    private final OrderItemRepository orderItemRepository;

    public ServicePackageService(LaundryServiceRepository serviceRepository, OrderItemRepository orderItemRepository) {
        this.serviceRepository = serviceRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public List<LaundryService> findAll() {
        return serviceRepository.findAllByOrderByNameAsc();
    }

    public List<LaundryService> findActive() {
        return serviceRepository.findByActiveTrueOrderByNameAsc();
    }

    public List<LaundryService> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        return serviceRepository.searchByName(query.trim());
    }

    public Optional<LaundryService> findById(Long id) {
        return serviceRepository.findById(id);
    }

    public LaundryService createService(String name, BigDecimal price, ServiceUnit unit, int estimatedDurationHours, boolean active) {
        validateServiceInput(name, price, unit, estimatedDurationHours);

        if (serviceRepository.existsByName(name.trim())) {
            throw new IllegalArgumentException("Layanan dengan nama '" + name.trim() + "' sudah ada.");
        }

        LaundryService service = new LaundryService(name.trim(), price, unit, estimatedDurationHours);
        service.setActive(active);
        return serviceRepository.save(service);
    }

    public LaundryService updateService(Long id, String name, BigDecimal price, ServiceUnit unit, int estimatedDurationHours, boolean active) {
        validateServiceInput(name, price, unit, estimatedDurationHours);

        LaundryService service = serviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Layanan tidak ditemukan"));

        serviceRepository.findByName(name.trim()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Layanan dengan nama '" + name.trim() + "' sudah terdaftar.");
            }
        });

        service.setName(name.trim());
        service.setPrice(price);
        service.setUnit(unit);
        service.setEstimatedDurationHours(estimatedDurationHours);
        service.setActive(active);
        return serviceRepository.save(service);
    }

    public LaundryService toggleActive(Long id) {
        LaundryService service = serviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Layanan tidak ditemukan"));
        service.setActive(!service.isActive());
        return serviceRepository.save(service);
    }

    public void deleteService(Long id) {
        LaundryService service = serviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Layanan tidak ditemukan"));

        if (orderItemRepository.existsByServiceId(id)) {
            throw new IllegalStateException("Layanan yang sudah digunakan dalam transaksi tidak dapat dihapus. Silakan nonaktifkan layanan ini.");
        }

        serviceRepository.delete(service);
    }

    private void validateServiceInput(String name, BigDecimal price, ServiceUnit unit, int estimatedDurationHours) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama layanan wajib diisi.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Harga harus lebih besar dari 0.");
        }
        if (unit == null) {
            throw new IllegalArgumentException("Satuan layanan wajib dipilih (kg atau pcs).");
        }
        if (estimatedDurationHours <= 0) {
            throw new IllegalArgumentException("Estimasi durasi pengerjaan harus lebih besar dari 0 jam.");
        }
    }
}
