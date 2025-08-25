package com.mcmp.o11ymanager.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "influx")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Setter
public class InfluxEntity {


  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String uid;
  private String url;
  private String database;
  private String retentionPolicy;
  private String username;
  private String password;
}
