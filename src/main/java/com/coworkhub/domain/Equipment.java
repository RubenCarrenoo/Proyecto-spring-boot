package com.coworkhub.domain;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

/** Equipamiento (proyector, pizarra, etc.). Su nombre es único (RN-11). */
@Entity
@Table(name = "equipment")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /** Lado inverso de Room.equipment ("y viceversa"). */
    @ManyToMany(mappedBy = "equipment", fetch = FetchType.LAZY)
    private Set<Room> rooms = new LinkedHashSet<>();

    public Equipment() {
    }

    public Equipment(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<Room> getRooms() { return rooms; }
}
