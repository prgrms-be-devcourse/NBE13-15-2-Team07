package com.kdt.nbe13152team07.Expense;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="expense")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;


}
