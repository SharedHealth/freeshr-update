package org.sharedhealth.freeshrUpdate.eventWorker;


import org.ict4h.atomfeed.jdbc.JdbcUtils;
import org.ict4h.atomfeed.transaction.AFTransactionWork;
import org.ict4h.atomfeed.transaction.AFTransactionWorkWithoutResult;
import org.sharedhealth.freeshrUpdate.atomFeed.AtomFeedSpringTransactionManager;
import org.sharedhealth.freeshrUpdate.domain.EncounterBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class EncounterMovementTracker {

    private static final String SCHEMA_NAME = "";
    private AtomFeedSpringTransactionManager txManager;

    @Autowired
    public EncounterMovementTracker(AtomFeedSpringTransactionManager txManager) {
        this.txManager = txManager;
    }

    public List<EncounterBundle> trackPatientEncounterMovement(final String mergedPatientId, final String activePatientId, final List<EncounterBundle> bundles) {
        System.out.println("Tracking patient merge");

        return txManager.executeWithTransaction(new AFTransactionWork<List<EncounterBundle>>() {
            @Override
            public List<EncounterBundle> execute() {
                //select count(*) from encounter_movement_status where from_patient = mergedPatientId
//                int numberOfPatientEncounters = getNumberOfPatientEncounters(mergedPatientId);
//                if (numberOfPatientEncounters > 0) return;

                List<EncounterMovement> encounterMovementsForPatient = getEncounterMovementsForPatient(mergedPatientId);
                //if count = 0, then insert all records
                if (encounterMovementsForPatient.isEmpty()) {
                    saveBundleTrackingInfo(bundles, mergedPatientId, activePatientId);
                    return bundles;
                } else {
                    List<EncounterBundle> results = new ArrayList<EncounterBundle>();
                    for (EncounterMovement encounterMovement : encounterMovementsForPatient) {
                        for (EncounterBundle aBundle : bundles) {
                            if (aBundle.getEncounterId().equals(encounterMovement.getEncounterId())
                                    && aBundle.getHealthId().equals(mergedPatientId)) {
                                results.add(aBundle);
                            }
                        }
                    }
                    return results;
                }
            }

            @Override
            public PropagationDefinition getTxPropagationDefinition() {
                return PropagationDefinition.PROPAGATION_REQUIRES_NEW;
            }
        });
    }

    private void saveBundleTrackingInfo(List<EncounterBundle> bundles, String mergedPatientId, String activePatientId) {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet resultSet = null;
        String insertSQL = String.format("insert into %s (from_patient, to_patient, encounter_id, reason) values (?, ?, ?, ?)",
                JdbcUtils.getTableName(SCHEMA_NAME, "encounter_movement_status"));
        try {
            connection = txManager.getConnection();
            stmt = connection.prepareStatement(insertSQL);
            for (EncounterBundle bundle : bundles) {
                stmt.setString(1, mergedPatientId);
                stmt.setString(2, activePatientId);
                stmt.setString(3, bundle.getEncounterId());
                stmt.setString(4, "merge");
                stmt.executeUpdate();
            }
        } catch (SQLException sqe) {
            throw new RuntimeException(sqe);
        } finally {
            closeAll(stmt, resultSet);
        }
    }

    public List<EncounterMovement> getEncounterMovementsForPatient(final String patientId) {
        return txManager.executeWithTransaction(new AFTransactionWork<List<EncounterMovement>>() {
            @Override
            public List<EncounterMovement> execute() {
                Connection connection = null;
                PreparedStatement stmt = null;
                ResultSet resultSet = null;
                try {
                    connection = txManager.getConnection();
                    String sql = String.format("select id, from_patient, to_patient, encounter_id, reason from %s where from_patient = ? order by id",
                            JdbcUtils.getTableName(SCHEMA_NAME, "encounter_movement_status"));
                    stmt = connection.prepareStatement(sql);
                    stmt.setString(1, patientId);
                    resultSet = stmt.executeQuery();
                    EncounterMovement encounterMovement = null;

                    List<EncounterMovement> movementList = new ArrayList<EncounterMovement>();
                    while (resultSet.next()) {
                        movementList.add(new EncounterMovement(
                                resultSet.getInt("id"),
                                resultSet.getString("from_patient"),
                                resultSet.getString("to_patient"),
                                resultSet.getString("encounter_id"),
                                resultSet.getString("reason")));

                    }
                    return movementList;
                } catch (SQLException sqe) {
                    throw new RuntimeException(sqe);
                } finally {
                    closeAll(stmt, resultSet);
                }
            }

            @Override
            public PropagationDefinition getTxPropagationDefinition() {
                return PropagationDefinition.PROPAGATION_REQUIRES_NEW;
            }
        });
    }

    public EncounterMovement getEncounterForMovement(final String encounterId) {
        return txManager.executeWithTransaction(new AFTransactionWork<EncounterMovement>() {
            @Override
            public EncounterMovement execute() {
                Connection connection = null;
                PreparedStatement stmt = null;
                ResultSet resultSet = null;
                try {
                    connection = txManager.getConnection();
                    String sql = String.format("select id, from_patient, to_patient, encounter_id, reason from %s where encounter_id = ?",
                            JdbcUtils.getTableName(SCHEMA_NAME, "encounter_movement_status"));
                    stmt = connection.prepareStatement(sql);
                    stmt.setString(1, encounterId);
                    resultSet = stmt.executeQuery();
                    EncounterMovement encounterMovement = null;
                    if (resultSet.next()) {
                        encounterMovement = new EncounterMovement(
                                resultSet.getInt("id"),
                                resultSet.getString("from_patient"),
                                resultSet.getString("to_patient"),
                                resultSet.getString("encounter_id"),
                                resultSet.getString("reason"));

                    }
                    return encounterMovement;
                } catch (SQLException sqe) {
                    throw new RuntimeException(sqe);
                } finally {
                    closeAll(stmt, resultSet);
                }
            }

            @Override
            public PropagationDefinition getTxPropagationDefinition() {
                return PropagationDefinition.PROPAGATION_REQUIRES_NEW;
            }
        });
    }

    public void doneMovingEncounter(final EncounterMovement encToMove) {
        System.out.println("Done with Encounter" + encToMove.getEncounterId());
        txManager.executeWithTransaction(new AFTransactionWorkWithoutResult() {
            @Override
            protected void doInTransaction() {
                Connection connection = null;
                PreparedStatement stmt = null;
                ResultSet resultSet = null;
                String deleteSQL = String.format("delete from %s where id = ?",
                        JdbcUtils.getTableName(SCHEMA_NAME, "encounter_movement_status"));
                try {
                    connection = txManager.getConnection();
                    stmt = connection.prepareStatement(deleteSQL);
                    stmt.setInt(1, encToMove.getId());
                    stmt.executeUpdate();
                } catch (SQLException sqe) {
                    throw new RuntimeException(sqe);
                } finally {
                    closeAll(stmt, resultSet);
                }
            }

            @Override
            public PropagationDefinition getTxPropagationDefinition() {
                return PropagationDefinition.PROPAGATION_REQUIRES_NEW;
            }
        });
    }

    private int getNumberOfPatientEncounters(String patientId) {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet resultSet = null;
        try {
            connection = txManager.getConnection();
            String sql = String.format("select count(id) from %s where from_patient = ?",
                    JdbcUtils.getTableName(SCHEMA_NAME, "encounter_movement_status"));
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, patientId);
            resultSet = stmt.executeQuery();
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException sqe) {
            throw new RuntimeException(sqe);
        } finally {
            closeAll(stmt, resultSet);
        }

    }

    private void closeAll(PreparedStatement stmt, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void doneMovingEncounter(final String encounterId, final String associatedPatientId) {
        System.out.println(String.format("marking movement Encounter [%s] to Patient [%s] as done ..", encounterId, associatedPatientId));
        txManager.executeWithTransaction(new AFTransactionWorkWithoutResult() {
            @Override
            protected void doInTransaction() {
                Connection connection = null;
                PreparedStatement stmt = null;
                ResultSet resultSet = null;
                String deleteSQL = String.format("delete from %s where encounter_id = ? and to_patient = ?",
                        JdbcUtils.getTableName(SCHEMA_NAME, "encounter_movement_status"));
                try {
                    connection = txManager.getConnection();
                    stmt = connection.prepareStatement(deleteSQL);
                    stmt.setString(1, encounterId);
                    stmt.setString(2, associatedPatientId);
                    stmt.executeUpdate();
                    System.out.println(String.format("Done moving Encounter [%s] to Patient [%s]", encounterId, associatedPatientId));
                } catch (SQLException sqe) {
                    throw new RuntimeException(sqe);
                } finally {
                    closeAll(stmt, resultSet);
                }
            }

            @Override
            public PropagationDefinition getTxPropagationDefinition() {
                return PropagationDefinition.PROPAGATION_REQUIRES_NEW;
            }
        });
    }

    public int pendingNumberOfEncounterMovements(String patientId, String source) {
        String searchField = source.equalsIgnoreCase("from") ? "from_patient" : "to_patient";
        String selectQuery = String.format("select count(id) from %s where %s = ?",
                JdbcUtils.getTableName(SCHEMA_NAME, "encounter_movement_status"), searchField);
        try (PreparedStatement stmt = txManager.getConnection().prepareStatement(selectQuery)) {
            stmt.setString(1, patientId);
            ResultSet result = stmt.executeQuery();
            return result.next() ? result.getInt(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
