package uo.ri.cws.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import uo.ri.cws.domain.base.BaseEntity;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.assertion.StateChecks;
import uo.ri.util.math.Round;

/**
 * Titulo: Clase Invoice
 * Descripción: Contiene la entidad que corresponde a la factura 
 *
 * @author Omar Teixeira González, UO281847
 * @version 12 nov 2022
 */
@Entity
@Table(name = "TInvoices")
public class Invoice extends BaseEntity {
	/**
	 * Titulo: Enumeración InvoiceStatus
	 * Descripción: 
	 *
	 * @author Omar Teixeira González, UO281847
	 * @version 12 nov 2022
	 */
	public enum InvoiceStatus {
		/**
		 * Estado NOT_YET_PAID
		 */
		NOT_YET_PAID, 
		/**
		 * Estado PAID
		 */
		PAID
	}
	
	/**
	 * Constante DATE_VAT
	 */
	private static final LocalDate DATE_LIMIT = LocalDate.of(2012, 7, 1);
	/**
	 * Constante LOWER_PERCENTAGE
	 */
	private static final double LOWER_PERCENTAGE = 0.18;
	/**
	 * Constante UPPER_PERCENTAGE
	 */
	private static final double HIGHER_PERCENTAGE = 0.21;
	
	// natural attributes
	/**
	 * Atributo number
	 */
	@Column(unique = true)
	@Basic(optional = false)
	private Long number;
	/**
	 * Atributo date
	 */
	@Basic(optional = false)
	private LocalDate date;
	/**
	 * Atributo amount
	 */
	@Basic(optional = false)
	private double amount;
	/**
	 * Atributo vat
	 */
	@Basic(optional = false)
	private double vat;
	/**
	 * Atributo status
	 */
	@Enumerated(EnumType.STRING)
	private InvoiceStatus status = InvoiceStatus.NOT_YET_PAID;

	// accidental attributes
	/**
	 * Atributo workOrders
	 */
	@OneToMany(mappedBy = "invoice")
	private Set<WorkOrder> workOrders = new HashSet<>();
	/**
	 * Atributo charges
	 */
	@OneToMany(mappedBy = "invoice")
	private Set<Charge> charges = new HashSet<>();

	/**
	 * Constructor sin parámetros de la clase Invoice
	 */
	Invoice() {}

	/**
	 * Constructor con el número de la factura como parámetro de la clase Invoice
	 * 
	 * @param number, número de la factura
	 */
	public Invoice(Long number) {
		this(number, LocalDate.now(), new ArrayList<WorkOrder>());
	}

	/**
	 * Constructor con el número y fecha de la factura como parámetros de la 
	 * clase Invoice
	 * 
	 * @param number, número de la factura
	 * @param date, fecha de la factura
	 */
	public Invoice(Long number, LocalDate date) {
		this(number, date, new ArrayList<WorkOrder>());
	}
	
	/**
	 * Constructor con el número y las averías de la factura como parámetros de 
	 * la clase Invoice
	 * 
	 * @param number, número de la factura
	 * @param workOrders, averías de la factura
	 */
	public Invoice(Long number, List<WorkOrder> workOrders) {
		this(number, LocalDate.now(), workOrders);
	}
	
	// full constructor
	/**
	 * Constructor con el número, la fecha y las averías de la factura como 
	 * parámetros de la clase Invoice
	 * 
	 * @param number, número de la factura
	 * @param date, fecha de la factura
	 * @param workOrders, averías de la factura
	 */
	public Invoice(Long number, LocalDate date, List<WorkOrder> workOrders) {
		ArgumentChecks.isTrue(number >= 0, "El número de la factura debe ser mayor o igual a cero");
		ArgumentChecks.isNotNull(date, "La fecha de la factura no puede ser null");
		ArgumentChecks.isNotNull(workOrders, "Las averías de la factura no pueden ser null");

		this.number = number;
		this.date = date;
		this.workOrders.addAll(workOrders);
	}
	
	/**
	 * Método computeAmount
	 * Calcula la cantidad computada de las averías realizadas en la factura
	 * El IVA depende de la fecha
	 */
	private void computeAmount() {
		double amountComputed = 0.0;
		for (WorkOrder workOrder : workOrders) {
			amountComputed += workOrder.getAmount();
		}
		this.vat = date.isBefore(DATE_LIMIT) ? amountComputed*LOWER_PERCENTAGE : amountComputed*HIGHER_PERCENTAGE;
		this.amount = Round.twoCents(amountComputed);
	}

	/**
	 * Método addWorkOrder
	 * Añade una avería a la factura 
	 */
	public void addWorkOrder(WorkOrder workOrder) {
		// asserts the invoice is in NOT_YET_PAID status
		// links with the work order
		// marks the work order as INVOICED
		// computes the amount
		StateChecks.isTrue(isNotSettled());
		Associations.ToInvoice.link(this, workOrder);
		workOrder.markAsInvoiced();
		computeAmount();
	}

	/**
	 * Método removeWorkOrder
	 * Elimina una avería a la factura
	 */
	public void removeWorkOrder(WorkOrder workOrder) {
		// asserts the invoice is in NOT_YET_PAID status
		// unlinks with the work order
		// marks the work order as FINISHED
		// computes the amount
		StateChecks.isTrue(isNotSettled());
		Associations.ToInvoice.unlink(this, workOrder);
		workOrder.markAsFinished();
		computeAmount();
	}

	/**
	 * Método settle
	 * Modifica la factura a pagad (PAID)
	 */
	public void settle() {
		// asserts the invoice is in NOT_YET_PAID status
		// asserts the total amount from the charges is higher than the invoice amount
		// sets status to paid
		StateChecks.isTrue(isNotSettled());
		StateChecks.isTrue(getChargesAmount() >= amount);
		setStatus(InvoiceStatus.PAID);
	}
	
	/**
	 * Método isNotSettled
	 * Devuelve true si el estado es no pagado (NOT_YET_PAID)
	 * 
	 * @return true o false, si el estado es no pagado (NOT_YET_PAID)
	 */
	public boolean isNotSettled() {
		return status == InvoiceStatus.NOT_YET_PAID;
	}

	/**
	 * Método getChargesAmount
	 * Devuelve la cantidad total de los cargos a pagar
	 * 
	 * @return chargeAmount, cargos a pagar
	 */
	public double getChargesAmount() {
		double chargeAmount = 0.0;
		for (Charge charge : charges) {
			chargeAmount += charge.getAmount();
		}
		return chargeAmount;
	}

	/**
	 * Método getNumber
	 * Devuelve el número de la factura
	 * 
	 * @return number, número de la factura
	 */
	public Long getNumber() {
		return number;
	}

	/**
	 * Método setNumber
	 * Modifica el número de la factura
	 * 
	 * @param number, número de la factura
	 */
	public void setNumber(Long number) {
		this.number = number;
	}

	/**
	 * Método getDate
	 * Devuelve la fecha de la factura
	 * 
	 * @return date, fecha de la factura
	 */
	public LocalDate getDate() {
		return date;
	}

	/**
	 * Método setDate
	 * Modifica la fecha de la factura
	 * 
	 * @param date, fecha de la factura
	 */
	public void setDate(LocalDate date) {
		this.date = date;
	}

	/**
	 * Método getAmount
	 * Devuelve la cantidad de la factura
	 * 
	 * @return amount, cantidad de la factura
	 */
	public double getAmount() {
		return amount;
	}

	/**
	 * Método setAmount
	 * Modifica la cantidad de la factura
	 * 
	 * @param amount, cantidad de la factura
	 */
	public void setAmount(double amount) {
		this.amount = amount;
	}
	
	/**
	 * Método getVat
	 * Devuelve el IVA de la factura
	 * 
	 * @return vat, IVA de la factura
	 */
	public double getVat() {
		return vat;
	}
	
	/**
	 * Método setVat
	 * Modifica el IVA de la factura
	 * 
	 * @param vat, IVA de la factura
	 */
	public void setVat(double vat) {
		this.vat = vat;
	}

	/**
	 * Método getStatus
	 * Devuelve el estado de la factura
	 *  
	 * @return status, estado de la factura
	 */
	public InvoiceStatus getStatus() {
		return status;
	}
	
	/**
	 * Método setStatus
	 * Modifica el estado de la factura
	 *  
	 * @param status, estado de la factura
	 */
	public void setStatus(InvoiceStatus status) {
		this.status = status;
	}

	/**
	 * Método getWorkOrders
	 * Devuelve las averías de la factura
	 * 
	 * @return workOrders, averías de la factura
	 */
	public Set<WorkOrder> getWorkOrders() {
		return new HashSet<>(workOrders);
	}

	/**
	 * Método _getWorkOrders
	 * Devuelve las averías de la factura para modificarlas (añadir o eliminar)
	 * 
	 * @return workOrders, averías de la factura
	 */
	Set<WorkOrder> _getWorkOrders() {
		return workOrders;
	}

	/**
	 * Método getCharges
	 * Devuelve los cargos de la factura
	 * 
	 * @return charges, cargos de la factura
	 */
	public Set<Charge> getCharges() {
		return new HashSet<>(charges);
	}

	/**
	 * Método _getCharges
	 * Devuelve los cargos de la factura para modificarlas (añadir o eliminar)
	 * 
	 * @return charges, cargos de la factura
	 */
	Set<Charge> _getCharges() {
		return charges;
	}

	/**
	 * Método hashCode 
	 * Devuelve el hashCode del objeto
	 * 
	 * @return hashCode, hashCode de la entidad
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(number);
		return result;
	}

	/**
	 * Método equals 
	 * Devuelve true si dos objetos son iguales
	 * 
	 * @return true o false, en función de si los objetos son iguales
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Invoice other = (Invoice) obj;
		return Objects.equals(number, other.number);
	}

	/**
	 * Método toString 
	 * Devuelve una cadena con los datos de la clase
	 * 
	 * @return string, cadena con los datos de la clase
	 */
	@Override
	public String toString() {
		return "Invoice [number=" + number + ", date=" + date + ", amount=" + amount + ", vat=" + vat + ", status="
				+ status + "]";
	}
}
