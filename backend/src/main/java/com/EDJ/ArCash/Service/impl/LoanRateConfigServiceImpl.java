package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.LoanRateConfigService;

import com.EDJ.ArCash.DTO.AuthDTO.LoanRatesResponse;
import com.EDJ.ArCash.DTO.AuthDTO.LoanRatesResponse.LoanRateItem;
import com.EDJ.ArCash.DTO.AuthDTO.LoanRatesUpdateRequest;
import com.EDJ.ArCash.Models.LoanRateConfig;
import com.EDJ.ArCash.Repository.LoanRateConfigRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LoanRateConfigServiceImpl implements LoanRateConfigService {

    public static final Set<Integer> ALLOWED_INSTALLMENTS = Set.of(3, 6, 12);
    public static final double MIN_RATE_PERCENT = 0.5;
    public static final double MAX_RATE_PERCENT = 15.0;

    private static final Map<Integer, Double> DEFAULT_RATES = Map.of(
            3, 0.030,
            6, 0.040,
            12, 0.055
    );

    private final LoanRateConfigRepository repository;

    public LoanRateConfigServiceImpl(LoanRateConfigRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    @Transactional
    public void seedDefaults() {
        for (Map.Entry<Integer, Double> entry : DEFAULT_RATES.entrySet()) {
            if (!repository.existsById(entry.getKey())) {
                repository.save(new LoanRateConfig(entry.getKey(), entry.getValue()));
            }
        }
    }

    @Transactional(readOnly = true)
    public double monthlyRateFor(int installments) {
        return repository.findById(installments)
                .map(LoanRateConfig::getMonthlyRate)
                .orElseGet(() -> {
                    Double fallback = DEFAULT_RATES.get(installments);
                    if (fallback == null) {
                        throw new IllegalArgumentException("Solo se permiten 3, 6 o 12 cuotas.");
                    }
                    return fallback;
                });
    }

    @Transactional(readOnly = true)
    public LoanRatesResponse listRates() {
        ensureDefaultsPresent();
        List<LoanRateConfig> configs = repository.findAllByOrderByInstallmentsAsc();
        String updatedAt = configs.stream()
                .map(LoanRateConfig::getUpdatedAt)
                .filter(v -> v != null && !v.isBlank())
                .max(String::compareTo)
                .orElse(null);

        List<LoanRateItem> items = configs.stream()
                .map(c -> new LoanRateItem(
                        c.getInstallments(),
                        c.getMonthlyRate(),
                        round2(c.getMonthlyRate() * 100.0)
                ))
                .toList();
        return new LoanRatesResponse(items, updatedAt);
    }

    @Transactional
    public LoanRatesResponse updateRates(LoanRatesUpdateRequest request) {
        if (request == null || request.getRates() == null || request.getRates().isEmpty()) {
            throw new IllegalArgumentException("Debés enviar las tasas a actualizar.");
        }

        ensureDefaultsPresent();

        for (LoanRatesUpdateRequest.LoanRateUpdateItem item : request.getRates()) {
            if (!ALLOWED_INSTALLMENTS.contains(item.getInstallments())) {
                throw new IllegalArgumentException(
                        "Plazo inválido: " + item.getInstallments() + ". Solo 3, 6 o 12 cuotas.");
            }
            double percent = item.getMonthlyRatePercent();
            if (percent < MIN_RATE_PERCENT || percent > MAX_RATE_PERCENT) {
                throw new IllegalArgumentException(
                        "La tasa de " + item.getInstallments()
                                + " cuotas debe estar entre "
                                + MIN_RATE_PERCENT + "% y " + MAX_RATE_PERCENT + "% mensual.");
            }
        }

        Map<Integer, Double> incoming = request.getRates().stream()
                .collect(Collectors.toMap(
                        LoanRatesUpdateRequest.LoanRateUpdateItem::getInstallments,
                        LoanRatesUpdateRequest.LoanRateUpdateItem::getMonthlyRatePercent,
                        (a, b) -> b
                ));

        List<LoanRateConfig> toSave = new ArrayList<>();
        for (Integer term : ALLOWED_INSTALLMENTS) {
            if (!incoming.containsKey(term)) {
                continue;
            }
            double decimal = round4(incoming.get(term) / 100.0);
            LoanRateConfig config = repository.findById(term)
                    .orElseGet(() -> new LoanRateConfig(term, decimal));
            config.setMonthlyRate(decimal);
            toSave.add(config);
        }
        repository.saveAll(toSave);
        return listRates();
    }

    private void ensureDefaultsPresent() {
        for (Map.Entry<Integer, Double> entry : DEFAULT_RATES.entrySet()) {
            if (!repository.existsById(entry.getKey())) {
                repository.save(new LoanRateConfig(entry.getKey(), entry.getValue()));
            }
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
