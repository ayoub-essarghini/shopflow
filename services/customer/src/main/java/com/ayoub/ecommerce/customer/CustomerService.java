package com.ayoub.ecommerce.customer;


import com.ayoub.ecommerce.customer.exception.CustomerNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public String createCusomter(@Valid CustomerRequest request) {
        var customer = customerRepository.save(customerMapper.toCustomer(request));
        return customer.getId();
    }

    public void updateCusomer(@Valid CustomerRequest request) {
        var customer = customerRepository.findById(request.id()).orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
        mergerCustomer(customer, request);
        customerRepository.save(customer);
    }

    private void mergerCustomer(Customer customer, @Valid CustomerRequest request) {
      if (request.firstName() != null) customer.setFirstName(request.firstName());
      if (request.lastName() != null) customer.setLastName(request.lastName());
      if (request.email() != null) customer.setEmail(request.email());
      if (request.address() != null) customer.setAddress(request.address());
    }

    public List<CustomerResponse> findAllCustomers() {
        return customerRepository.findAll().stream().map(customerMapper::fromCustomer).toList();
    }

    public Boolean existsCustomer(String customerId) {
        return customerRepository.findById(customerId).isPresent();
    }

    public CustomerResponse finById(String customerId) {
        return customerRepository.findById(customerId).map(customerMapper::fromCustomer).orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
    }

    public void deleteById(String customerId) {
        customerRepository.deleteById(customerId);
    }
}
