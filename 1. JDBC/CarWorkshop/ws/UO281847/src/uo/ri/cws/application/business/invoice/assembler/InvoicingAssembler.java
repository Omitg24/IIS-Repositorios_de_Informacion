package uo.ri.cws.application.business.invoice.assembler;

import java.util.ArrayList;
import java.util.List;

import uo.ri.cws.application.business.invoice.InvoicingService.InvoiceBLDto;
import uo.ri.cws.application.business.invoice.InvoicingService.InvoiceBLDto.InvoiceState;
import uo.ri.cws.application.business.invoice.InvoicingService.WorkOrderForInvoicingBLDto;
import uo.ri.cws.application.persistence.invoice.InvoiceGateway.InvoiceDALDto;
import uo.ri.cws.application.persistence.workorder.WorkOrderGateway.WorkOrderDALDto;

/**
 * Titulo: Clase InvoicingAssembler
 *
 * @author Omar Teixeira Gonz�lez, UO281847
 * @version 13 oct 2022
 */
public class InvoicingAssembler {
	/**
	 * M�todo toDto Pasa un InvoiceDALDto a un InvoiceBLDto
	 * 
	 * @param arg
	 * @return result
	 */
	public static InvoiceBLDto toDto(InvoiceDALDto arg) {
		InvoiceBLDto result = new InvoiceBLDto();
		result.id = arg.id;
		result.number = arg.number;
		result.state = InvoiceState.valueOf(arg.state);
		result.date = arg.date;
		result.total = arg.amount;
		result.vat = arg.vat;
		return result;
	}

	/**
	 * M�todo toInvoicingWorkOrderList
	 * Pasa una lista de WorkOrderDALDto a WorkOrderForInvoicingBLDto
	 * @param arg
	 * @return result
	 */
	public static List<WorkOrderForInvoicingBLDto> toInvoicingWorkOrderList(
			List<WorkOrderDALDto> arg) {
		List<WorkOrderForInvoicingBLDto> result = new ArrayList<WorkOrderForInvoicingBLDto>();
		for (WorkOrderDALDto record : arg)
			result.add(toDto(record));
		return result;
	}

	/**
	 * M�todo toDto
	 * Pasa un WorkOrderDALDto a un WorkOrderForInvoicingBLDto
	 * @param record
	 * @return dto
	 */
	private static WorkOrderForInvoicingBLDto toDto(WorkOrderDALDto record) {
		WorkOrderForInvoicingBLDto dto = new WorkOrderForInvoicingBLDto();
		dto.id = record.id;
		dto.date = record.date;
		dto.description = record.description;
		dto.date = record.date;
		dto.state = record.state;
		dto.total = record.amount;

		return dto;
	}
}
