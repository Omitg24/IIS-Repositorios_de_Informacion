package uo.ri.cws.application.service.util.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import uo.ri.cws.application.business.mechanic.MechanicService.MechanicBLDto;

public class AddMechanicSqlUnitOfWork {

	private MechanicBLDto dto;
	private ConnectionData connectionData;
	private PreparedStatement insertIntoMechanics;

	public AddMechanicSqlUnitOfWork(MechanicBLDto dto) {
		this.connectionData = PersistenceXmlScanner.scan();
		this.dto = dto;
	}

	public void execute() {
		JdbcTransaction trx = new JdbcTransaction( connectionData );
		trx.execute((con) -> {
			prepareStatements( con );
			insertMechanic();
		});
	}

	private static final String INSERT_INTO_TMECHANICS =
			"INSERT INTO TMECHANICS"
				+ " ( ID, DNI, NAME, SURNAME, VERSION )"
				+ " VALUES ( ?, ?, ?, ?, ?)";

	private void insertMechanic() throws SQLException {
		PreparedStatement st = insertIntoMechanics;
		int i = 1;
		st.setString(i++, dto.id);
		st.setString(i++, dto.dni);
		st.setString(i++, dto.name);
		st.setString(i++, dto.surname);
		st.setLong(i++, dto.version);

		st.executeUpdate();
	}

	private void prepareStatements(Connection con) throws SQLException {
		insertIntoMechanics = con.prepareStatement(INSERT_INTO_TMECHANICS);
	}

}
