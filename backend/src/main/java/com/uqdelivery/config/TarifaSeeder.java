package com.uqdelivery.config;

import com.uqdelivery.domain.Tarifa;
import com.uqdelivery.repository.TarifaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * HU-05 · Crea la tarifa inicial si la tabla esta vacia (el proyecto usa ddl-auto, no migraciones).
 * El administrador la ajusta luego desde PUT /api/tarifas/{id}. Valores de ejemplo en COP.
 */
@Component
public class TarifaSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TarifaSeeder.class);

    private final TarifaRepository tarifas;

    public TarifaSeeder(TarifaRepository tarifas) {
        this.tarifas = tarifas;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (tarifas.count() > 0) return;
        tarifas.save(new Tarifa("Tarifa urbana estandar",
                new BigDecimal("5000.00"),  // tarifa base
                new BigDecimal("1200.00"),  // por km
                new BigDecimal("800.00"),   // por kg facturable
                new BigDecimal("3000.00"),  // recargo fragil
                new BigDecimal("5000.00"),  // recargo urgente
                new BigDecimal("1.00"),     // % seguro (reservado)
                true));
        log.info("TarifaSeeder: tarifa inicial creada");
    }
}
