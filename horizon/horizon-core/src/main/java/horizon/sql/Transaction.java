/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql;

import java.sql.Connection;

import javax.transaction.Status;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import horizon.base.AbstractComponent;
import horizon.base.Log;
import horizon.jndi.ObjectLocator;

/**Is a representation of database transaction.
 * <p>Horizon offers 3 types of Transaction you can use depending on the application requirements.
 * <ul><li>JDBC transaction that works with a {@link Connection}</li>
 *     <li>container-managed transaction that works with a {@link TransactionManager}</li>
 *     <li>bean-managed transaction that works with a {@link UserTransaction}</li>
 * </ul>
 * You do not create a Transaction yourself.<br />
 * You specify the type of Transaction in the configuration file(dbaccess.xml)
 * along with connection information for {@link DBAccess}.<br />
 * If you need other type of Transaction, you should define a {@link horizon.sql.Transaction.Factory} and
 * {@link DBAccess#setTransactionFactory(horizon.sql.Transaction.Factory) set it to the DBAccess} you work with.
 * </p>
 * <p>To start a database transaction in the example below,
 * you {@link DBAccess#transaction() get a Transaction from a DBAccess}
 * and call the {@link Transaction#begin() begin()} method.<br />
 * On completion, you can either {@link Transaction#commit() commit}
 * or {@link Transaction#rollback() roll back} the transaction depending on the result of it.
 * <pre><code> DBAccess dbaccess = ...;
 * try {
 *     dbaccess.transaction().begin();
 *     ...
 *     dbaccess.transaction().commit();
 * } catch (Exception e) {
 *     dbaccess.transaction().rollback();
 * }</code></pre>
 * You can make the code simpler with the {@link DBAccess#transact(horizon.sql.DBAccess.Try)}
 * and {@link DBAccess#transact(horizon.sql.DBAccess.TryReturn)} methods.
 * <pre><code> DBAccess dbaccess = ...;
 * dbaccess.transact(db -> {
 *     ...
 * });
 * //Or
 * int count = dbaccess.transact(db -> {
 *     ...
 *     return selected;
 * });
 * </code></pre>
 * </p>
 */
public abstract class Transaction extends AbstractComponent {
	enum Type {
		BMT,
		CMT,
		JDBC,
		XMT
	}

	static class Definition {
		private Type type;
		private String jndiName;

		public Type getType() {
			return type;
		}

		public Definition setType(Type type) {
			this.type = type;
			return this;
		}

		public String getJndiName() {
			return jndiName;
		}

		public Definition setJndiName(String jndiName) {
			this.jndiName = jndiName;
			return this;
		}
	}

	/**A factory to create a Transaction associated with the dbaccess
	 */
	@FunctionalInterface
	public interface Factory {
		/**Returns a Transaction associated with the dbaccess.
		 * @param dbaccess DBAccess
		 * @return Transaction associated with the dbaccess
		 */
		Transaction create(DBAccess dbaccess);
	}

	private static Transaction create(DBAccess dbaccess) {
		Definition def = DatasourceFactory.getTransactionDefinition(dbaccess.getDatasource());
		switch (def.getType()) {
		case BMT: return new BMTransaction().setJndiName(def.getJndiName());
		case CMT: return new CMTransaction().setJndiName(def.getJndiName());
		default: return new JDBCTransaction().setDBAccess(dbaccess);
		}
	}

	private static Transaction getTransaction() {
		return DBAccess.resources().get("transaction");
	}

	/**Returns a Transaction associated with the dbaccess.<br />
	 * Bound to the current thread, the Transaction is available for other objects in the current thread.
	 * @param dbaccess DBAccess
	 * @return Transaction associated with the dbaccess
	 */
	public static Transaction get(DBAccess dbaccess) {
		Transaction tx = getTransaction();
		if (tx != null) return tx;

		Factory factory = ifEmpty(dbaccess.getTransactionFactory(), () -> Transaction::create);
		DBAccess.resources().put("transaction", tx = factory.create(dbaccess));

		return tx;
	}

	/**Status of the transaction
	 */
	protected boolean busy;

	/**Creates a new Transaction.
	 */
	protected Transaction() {
		log().trace(() -> getClass().getSimpleName() + " created");
	}

	int getStatus() {
		return Status.STATUS_NO_TRANSACTION;
	}

	private void setBusy(boolean busy) {
		this.busy = busy;
	}

	/**Begins a transaction.
	 * @return
	 * <ul><li>true if the call to the method begins a transaction</li>
	 * 	   <li>false if a transaction is already begun</li>
	 * </ul>
	 */
	public boolean begin() {
		if (busy) return false;

		try {
			setBusy(true);
			boolean started = doBegin();
			if (started)
				log().debug(() -> "---------- " + getClass().getSimpleName() + " started ----------");
			return started;
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	/**Performs subclass-specific work to begin a transaction.
	 * @return
	 * <ul><li>true if the call to the method begins a transaction</li>
	 * 	   <li>false if a transaction is already begun</li>
	 * </ul>
	 * @throws Exception
	 */
	protected abstract boolean doBegin() throws Exception;

	/**Returns whether the transaction is committable.<br />
	 * A transaction is committable when the transaction is active and has no exceptions thrown.
	 * @return
	 * <ul><li>true if the transaction is committable</li>
	 * 	   <li>false otherwise</li>
	 * </ul>
	 */
	public boolean isCommittable() {
		return busy && ExceptionHolder.isEmpty();
	}

	/**Commits the transaction.
	 */
	public void commit() {
		if (!busy) return;

		try {
			if (isCommittable()) {
				doCommit();
				log().debug(() -> "---------- " + getClass().getSimpleName() + " committed ----------");
			} else
				rollback();
		} catch (Exception e) {
			throw runtimeException(e);
		} finally {
			setBusy(false);
		}
	}

	/**Performs subclass-specific work to commit the transaction.
	 * @throws Exception
	 */
	protected abstract void doCommit() throws Exception;

	/**Rolls back the transaction.
	 */
	public void rollback() {
		if (!busy) return;

		try {
			doRollback();
			log().debug(() -> "---------- " + getClass().getSimpleName() + " rolled back ----------");
		} catch (Exception e) {
			throw runtimeException(e);
		} finally {
			setBusy(false);
		}
	}

	/**Performs subclass-specific work to roll back the transaction.
	 * @throws Exception
	 */
	protected abstract void doRollback() throws Exception;

	/**Clears up the internal resources.
	 */
	protected abstract void clear();

	/**Releases the transaction from the current thread.
	 * @return a {@link Status} constant
	 */
	public static final int release() {
		Transaction tx = getTransaction();
		if (tx == null)
			return Status.STATUS_NO_TRANSACTION;
		else {
			DBAccess.resources().remove("transaction");
			int status = tx.getStatus();
			tx.clear();
			Class<?> klass = tx.getClass();
			Log.get(klass).trace(() -> klass.getSimpleName() + " released");
			return status;
		}
	}
}

class JDBCTransaction extends Transaction {
	private DBAccess dbaccess;
	private Connection connection;
	private int status = Status.STATUS_NO_TRANSACTION;

	public JDBCTransaction setDBAccess(DBAccess dbaccess) {
		this.dbaccess = dbaccess;
		return this;
	}

	@Override
	int getStatus() {
		return connection != null ? status : Status.STATUS_NO_TRANSACTION;
	}

	@Override
	protected boolean doBegin() throws Exception {
		if (connection == null) {
			connection = dbaccess.connection();
			status = Status.STATUS_ACTIVE;
			return true;
		} else
			return false;
	}

	@Override
	protected void doCommit() throws Exception {
		connection.commit();
		status = Status.STATUS_COMMITTED;
	}

	@Override
	protected void doRollback() throws Exception {
		connection.rollback();
		status = Status.STATUS_ROLLEDBACK;
	}

	@Override
	protected void clear() {
		dbaccess = null;
		connection = null;
	}
}

class BMTransaction extends Transaction {
	private String jndiName;
	private UserTransaction utx;

	BMTransaction setJndiName(String jndiName) {
		this.jndiName = jndiName;
		return this;
	}

	@Override
	int getStatus() {
		try {
			return utx != null ? utx.getStatus() : Status.STATUS_NO_TRANSACTION;
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	@Override
	protected boolean doBegin() throws Exception {
		if (utx == null)
			utx = ObjectLocator.get(null).lookup(jndiName);
		if (Status.STATUS_NO_TRANSACTION == utx.getStatus()) {
			utx.begin();
			return true;
		}
		return false;
	}

	@Override
	protected void doCommit() throws Exception {
		if (Status.STATUS_ACTIVE == utx.getStatus())
			utx.commit();
	}

	@Override
	protected void doRollback() throws Exception {
		if (Status.STATUS_ACTIVE == utx.getStatus())
			utx.rollback();
	}

	@Override
	protected void clear() {
		utx = null;
	}
}

class CMTransaction extends Transaction {
	private String jndiName;
	private TransactionManager tm;
	private boolean started;

	CMTransaction setJndiName(String jndiName) {
		this.jndiName = jndiName;
		return this;
	}

	@Override
	int getStatus() {
		try {
			return tm != null ? tm.getStatus() : Status.STATUS_NO_TRANSACTION;
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	@Override
	protected boolean doBegin() throws Exception {
		if (tm == null)
			tm = ObjectLocator.get(null).lookup(jndiName);
		if (Status.STATUS_NO_TRANSACTION == tm.getStatus()) {
			tm.begin();
			return started = true;
		}
		return started = false;
	}

	@Override
	protected void doCommit() throws Exception {
		if (started && Status.STATUS_ACTIVE == tm.getStatus()) {
			tm.commit();
		}
	}

	@Override
	protected void doRollback() throws Exception {
		if (Status.STATUS_ACTIVE == tm.getStatus()) {
			tm.setRollbackOnly();
		}
	}

	@Override
	protected void clear() {
		tm = null;
		started = false;
	}
}