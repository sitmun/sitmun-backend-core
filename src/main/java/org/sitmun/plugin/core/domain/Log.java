package org.sitmun.plugin.core.domain;

import java.math.BigInteger;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "STM_LOG")
public class Log {

  @TableGenerator(
      name = "STM_LOG_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "LOG_CODIGO",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_LOG_GEN")
  @Column(name = "LOG_CODIGO", precision = 11)
  private BigInteger id;

  @Column(name = "LOG_TIPO", length = 50)
  private String nombre;

  @Column(name = "LOG_CODUSU", precision = 11)
  private BigInteger codigoUsuario;

  @Column(name = "LOG_CODAPP", precision = 11)
  private BigInteger codigoAplicacion;

  @Column(name = "LOG_CODTER", precision = 11)
  private BigInteger codigoTerritorio;

  @Column(name = "LOG_CODTAR", precision = 11)
  private BigInteger codigoTarea;

  @Column(name = "LOG_FECHA")
  @Temporal(TemporalType.TIMESTAMP)
  private Date fecha;

  //TODO log_cont es el identificador del log?
  @Column(name = "LOG_CONT", precision = 11)
  private BigInteger cont;


  /**
   * @return the id
   */
  public BigInteger getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(BigInteger id) {
    this.id = id;
  }

  /**
   * @return the nombre
   */
  public String getNombre() {
    return nombre;
  }

  /**
   * @param nombre the nombre to set
   */
  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  /**
   * @return the codigoUsuario
   */
  public BigInteger getCodigoUsuario() {
    return codigoUsuario;
  }

  /**
   * @param codigoUsuario the codigoUsuario to set
   */
  public void setCodigoUsuario(BigInteger codigoUsuario) {
    this.codigoUsuario = codigoUsuario;
  }

  /**
   * @return the codigoAplicacion
   */
  public BigInteger getCodigoAplicacion() {
    return codigoAplicacion;
  }

  /**
   * @param codigoAplicacion the codigoAplicacion to set
   */
  public void setCodigoAplicacion(BigInteger codigoAplicacion) {
    this.codigoAplicacion = codigoAplicacion;
  }

  /**
   * @return the codigoTerritorio
   */
  public BigInteger getCodigoTerritorio() {
    return codigoTerritorio;
  }

  /**
   * @param codigoTerritorio the codigoTerritorio to set
   */
  public void setCodigoTerritorio(BigInteger codigoTerritorio) {
    this.codigoTerritorio = codigoTerritorio;
  }

  /**
   * @return the codigoTarea
   */
  public BigInteger getCodigoTarea() {
    return codigoTarea;
  }

  /**
   * @param codigoTarea the codigoTarea to set
   */
  public void setCodigoTarea(BigInteger codigoTarea) {
    this.codigoTarea = codigoTarea;
  }

  /**
   * @return the fecha
   */
  public Date getFecha() {
    return fecha;
  }

  /**
   * @param fecha the fecha to set
   */
  public void setFecha(Date fecha) {
    this.fecha = fecha;
  }

  public BigInteger getCont() {
    return cont;
  }

  public void setCont(BigInteger cont) {
    this.cont = cont;
  }

}
