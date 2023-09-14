package uo.ri.cws.domain;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import uo.ri.cws.domain.base.BaseEntity;
import uo.ri.util.assertion.ArgumentChecks;
import uo.ri.util.assertion.StateChecks;
import uo.ri.util.math.Round;

/**
 * Titulo: Clase WorkOrder
 * Descripción: 
 *
 * @author Omar Teixeira González, UO281847
 * @version 12 nov 2022
 */
@Entity
@Table(name = "TWorkOrders", uniqueConstraints = { 
		@UniqueConstraint(columnNames = { 
				"DATE", "VEHICLE_ID" 
			}
		) 
	}
)
public class WorkOrder extends BaseEntity {
	/**
	 * Titulo: Enumeración WorkOrderStatus
	 * Descripción: 
	 *
	 * @author Omar Teixeira González, UO281847
	 * @version 12 nov 2022
	 */
	public enum WorkOrderStatus {
		/**
		 * Estado OPEN
		 */
		OPEN, 
		/**
		 * Estado ASSIGNED
		 */
		ASSIGNED, 
		/**
		 * Estado FINISHED
		 */
		FINISHED, 
		/**
		 * Estado INVOICED
		 */
		INVOICED
	}

	// natural attributes
	/**
	 * Atributo date
	 */
	@Column(unique = true)
	@Basic(optional = false)
	private LocalDateTime date;
	/**
	 * Atributo description
	 */
	@Basic(optional = false)
	private String description;
	/**
	 * Atributo amount
	 */
	@Basic(optional = false)
	private double amount = 0.0;
	/**
	 * Atributo status
	 */
	@Enumerated(EnumType.STRING)
	private WorkOrderStatus status = WorkOrderStatus.OPEN;

	// accidental attributes
	/**
	 * Atributo vehicle
	 */
	@ManyToOne(optional = false)
	private Vehicle vehicle;
	/**
	 * Atributo mechanic
	 */
	@ManyToOne
	private Mechanic mechanic;
	/**
	 * Atributo invoice
	 */
	@ManyToOne
	private Invoice invoice;
	/**
	 * Atributo interventions
	 */
	@OneToMany(mappedBy = "workOrder")
	private Set<Intervention> interventions = new HashSet<>();

	/**
	 * Constructor sin parámetros de la clase WorkOrder
	 */
	WorkOrder() {}

	/**
	 * Constructor con el vehículo de la avería como parámetros de la 
	 * clase WorkOrder
	 * 
	 * @param vehicle, vehículo averiado
	 */
	public WorkOrder(Vehicle vehicle) {
		this(vehicle, LocalDateTime.now());
	}
	
	/**
	 * Constructor con el vehículo y la fecha de la avería como parámetros de 
	 * la clase WorkOrder
	 * 
	 * @param vehicle, vehículo averiado
	 * @param date, fecha de la avería
	 */
	public WorkOrder(Vehicle vehicle, LocalDateTime date) {
		this(vehicle, date, "no-description");
	}
	
	
	/**
	 * Constructor con el vehículo y la descripción de la avería como parámetros 
	 * de la clase WorkOrder
	 * 
	 * @param vehicle, vehículo averiado
	 * @param description, descripción de la avería
	 */
	public WorkOrder(Vehicle vehicle, String description) {
		this(vehicle, LocalDateTime.now(), description);
	}

	/**
	 * Constructor con el vehículo, la fecha y la descripción de la avería como 
	 * parámetros de la clase WorkOrder
	 * 
	 * @param vehicle, vehículo averiado
	 * @param date, fecha de la avería
	 * @param description, descripción de la avería
	 */
	public WorkOrder(Vehicle vehicle, LocalDateTime date, String description) {
		ArgumentChecks.isNotNull(vehicle, "El vehículo no puede ser null");
		ArgumentChecks.isNotNull(date, "La fecha de la avería no puede ser null");
		ArgumentChecks.isNotEmpty(description, "La descripción de la avería no puede ser null ni estar vacía");

		this.date = date.truncatedTo(ChronoUnit.MILLIS);
		this.description = description;

		Associations.Fix.link(vehicle, this);
	}

	/**
	 * Método computeAmount
	 * Calcula la cantidad computada de las intervenciones realizadas en la avería
	 */
	private void computeAmount() {
		double amountComputed = 0.0;
		for (Intervention intervention : interventions) {
			amountComputed += intervention.getAmount();
		}
		this.amount = Round.twoCents(amountComputed);
	}
	
	/**
	 * Método markAsInvoiced
	 * Modifica la avería a facturada (INVOICED)	  
	 */
	public void markAsInvoiced() {
		// asserts the work order is in FINISHED status
		// asserts the invoice is not null
		// marks the workorder as invoiced
		StateChecks.isTrue(isFinished());
		StateChecks.isNotNull(invoice);
		setStatus(WorkOrderStatus.INVOICED);
	}

	/**
	 * Método markAsFinished
	 * Modifica la avería a finalizada (FINISHED)
	 */
	public void markAsFinished() {
		// asserts the work order is in ASSIGNED status
		// asserts the work order is not linked with a mechanic
		// marks the workorder as FINISHED
		// computes the amount
		StateChecks.isTrue(isAssigned());
		StateChecks.isTrue(mechanic != null);
		setStatus(WorkOrderStatus.FINISHED);
		computeAmount();
	}

	/**
	 * Método markBackToFinished
	 * Modifica la avería a finished (FINISHED)
	 */
	public void markBackToFinished() {
		// asserts the work order is in INVOICED status
		// asserts the work order is still linked with a mechanic
		// marks the workorder as FINISHED
		StateChecks.isTrue(isInvoiced());
		StateChecks.isTrue(mechanic == null);
		setStatus(WorkOrderStatus.FINISHED);
	}

	/**
	 * Método assignTo
	 * Asigna la avería a un mecánico, marcándola como asignada
	 */
	public void assignTo(Mechanic mechanic) {
		// asserts the work order is in OPEN status
		// asserts the work order is not linked with a mechanic
		// links with the mechanic
		// sets status to ASSIGNED
		StateChecks.isTrue(isOpen());
		StateChecks.isTrue(mechanic == null);
		Associations.Assign.link(mechanic, this);
		setStatus(WorkOrderStatus.ASSIGNED);
	}

	/**
	 * Método desassign
	 * Desasigna la avería de un mecánico, marcándola  como abierta
	 */
	public void desassign() {
		// asserts the work order is in ASSIGNED status
		// unlinks with the mechanic
		// sets status to OPEN
		StateChecks.isTrue(isAssigned());
		Associations.Assign.unlink(mechanic, this);
		setStatus(WorkOrderStatus.OPEN);
	}

	/**
	 * Método reopen
	 * Vuelve a abrir la avería, desasignándola de un mecánico
	 */
	public void reopen() {
		// asserts the work order is in FINISHED status
		// sets status to OPEN
		// unlinks with the mechanic
		StateChecks.isTrue(isFinished());
		setStatus(WorkOrderStatus.OPEN);
		Associations.Assign.unlink(mechanic, this);
	}

	/**
	 * Método isFinished
	 * Devuelve true o false dependiendo de si el estado es FINISHED
	 * 
	 * @return true o false, si el estado es FINISHED
	 */
	public boolean isFinished() {
		return status.equals(WorkOrderStatus.ASSIGNED);
	}
	
	/**
	 * Método isAssigned
	 * Devuelve true o false dependiendo de si el estado es ASSIGNED
	 * 
	 * @return true o false, si el estado es ASSIGNED
	 */
	public boolean isAssigned() {
		return status.equals(WorkOrderStatus.ASSIGNED);
	}
	
	/**
	 * Método isInvoiced
	 * Devuelve true o false dependiendo de si el estado es INVOICED
	 * 
	 * @return true o false, si el estado es INVOICED
	 */
	public boolean isInvoiced() {
		return status.equals(WorkOrderStatus.INVOICED);
	}
	
	/**
	 * Método isOpen
	 * Devuelve true o false dependiendo de si el estado es OPEN
	 * 
	 * @return true o false, si el estado es OPEN
	 */
	public boolean isOpen() {
		return status.equals(WorkOrderStatus.OPEN);
	}
	
	/**
	 * Método getInterventions
	 * Devuelve las intervenciones de las averías
	 * 
	 * @return interventions, intervenciones de las averías
	 */
	public Set<Intervention> getInterventions() {
		return new HashSet<>(interventions);
	}

	/**
	 * Método _getInterventions
	 * Devuelve las intervenciones de las averías para modificarlas 
	 * (añadir o eliminar)
	 * 
	 * @return interventions, intervenciones de las averías
	 */
	Set<Intervention> _getInterventions() {
		return interventions;
	}

	/**
	 * Método getVehicle
	 * Devuelve el vehiculo de la avería
	 * 
	 * @return vehicle, vehiculo de la avería
	 */
	public Vehicle getVehicle() {
		return vehicle;
	}
	
	/**
	 * Método _getVehicle
	 * Modifica el vehiculo de la avería
	 * 
	 * @param vehicle, vehiculo de la avería
	 */
	void _setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	/**
	 * Método getMechanic
	 * Devuelve el mecánico que va a reparar la avería
	 * 
	 * @return mechanic, mecánico que va a repar la avería
	 */
	public Mechanic getMechanic() {
		return mechanic;
	}
	
	/**
	 * Método _setMechanic
	 * Modifica el mecánico que va a reparar la avería
	 * 
	 * @param mechanic, mecánico que va a reparar la avería
	 */
	void _setMechanic(Mechanic mechanic) {
		this.mechanic = mechanic;
	}

	/**
	 * Método getInvoice
	 * Devuelve la factura de la avería
	 * 
	 * @return invoice, factura de la avería
	 */
	public Invoice getInvoice() {
		return invoice;
	}
	
	/**
	 * Método _setInvoice
	 * Modifica la factura de la avería
	 * 
	 * @param invoice, factura de la avería
	 */
	void _setInvoice(Invoice invoice) {
		this.invoice = invoice;
	}

	/**
	 * Método getDate
	 * Devuelve la fecha de la avería
	 * 
	 * @return date, fecha de la avería
	 */
	public LocalDateTime getDate() {
		return date;
	}

	/**
	 * Método setDate
	 * Devuelve la fecha de la avería
	 * 
	 * @param date, fecha de la avería
	 */
	public void setDate(LocalDateTime date) {
		this.date = date;
	}

	/**
	 * Método getDescription
	 * Devuelve la descripción de la avería
	 * 
	 * @return description, descripción de la avería
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Método setDescription
	 * Modifica la description de la avería
	 * 
	 * @param description, descripción de la avería
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Método getAmount
	 * Devuelve la cantidad de la avería
	 * 
	 * @return amount, cantidad de la avería
	 */
	public double getAmount() {
		return amount;
	}

	/**
	 * Método setAmount
	 * Modifica la cantidad de la avería
	 * 
	 * @param amount, cantidad de la avería
	 */
	public void setAmount(double amount) {
		this.amount = amount;
	}

	/**
	 * Método getStatus
	 * Devuelve el estado de la avería
	 * 
	 * @return status, estado de la avería
	 */
	public WorkOrderStatus getStatus() {
		return status;
	}

	/**
	 * Método setStatus
	 * Modifica el estado de la avería
	 * 
	 * @param status, estado de la avería
	 */
	public void setStatus(WorkOrderStatus status) {
		this.status = status;
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
		result = prime * result + Objects.hash(date, vehicle);
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
		WorkOrder other = (WorkOrder) obj;
		return Objects.equals(date, other.date) && Objects.equals(vehicle, other.vehicle);
	}
	
	/**
	 * Método toString 
	 * Devuelve una cadena con los datos de la clase
	 * 
	 * @return string, cadena con los datos de la clase
	 */
	@Override
	public String toString() {
		return "WorkOrder [date=" + date + ", description=" + description + ", amount=" + amount + ", status=" + status
				+ ", vehicle=" + vehicle + ", mechanic=" + mechanic + ", invoice=" + invoice + "]";
	}	
}
