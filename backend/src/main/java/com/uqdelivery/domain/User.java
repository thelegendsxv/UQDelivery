package com.uqdelivery.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    //Campos agregados de la HU-02
    @Column(length = 20)
    private String telefono;

    @Column(length = 200)
    private String direccion;

    /** Un administrador puede desactivar cuentas; un usuario inactivo no puede iniciar sesion. */
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean activo = true;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected User() { }

    public User(String nombre, String email, String passwordHash, Role role) {
        this.nombre = nombre;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRole(Role role) { this.role = role; }

    //HU-02
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Instant getUpdatedAt() { return updatedAt; }

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}
