package com.motorola.oobtestapp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DBAccess {

	private final String DEST_FILE_PATH = "D:\\Log_file\\";

	String DeviceIP;

	java.sql.Connection myConn = null;
	private String DBIP;
	private String DBUser;
	private String DBPass;
	private int DBType;
	private String DeviceMAC;
	private String ResultsTableName = null;
	int C_TCIndex;

	public enum EMSTAFResult {
		Database_Opened, Database_Opened_Failed, Table_Created, Table_Exist, Table_Creation_Failed, Insert_Success, Insert_Failed, Delete_Success, Delete_Failed, Database_Closed, Database_Closed_Failed, WriteLogFile_Success, WriteLogFile_Failed, SUCCESS, FAILURE, Table_DoesNotExist, Exception_Occured, File_Not_Found, Execute_SQL_Success, Execute_SQL_Failure, Wrong_Intent, No_More_Records
	}

	public String getResultTableName() {
		return ResultsTableName;
	}

	public DBAccess(String DBIP, String DBUser, String DBPass, int DBType,
			String MAC) {
		this.DBIP = DBIP;
		this.DBUser = DBUser;
		this.DBPass = DBPass;
		this.DBType = DBType;
		this.DeviceMAC = MAC;
	}

	public List<AllSelectedTC> getTCsList(String SQL, String choice) {
		List<AllSelectedTC> TCs = new ArrayList<AllSelectedTC>();
		Statement stmt = null;
		ResultSet res;
		try {
			if (OpenConnection() == "Database_Opened") {
				stmt = myConn.createStatement();
				res = stmt.executeQuery(SQL);
				if (choice.equalsIgnoreCase("selected")) {
					while (res.next()) {
						AllSelectedTC Tc = new AllSelectedTC();
						Tc.FillTestParameters(res.getString("TestID"),
								res.getString("SelectedTCID"), null, null,
								null, null,
								Integer.parseInt(res.getString("RowNum")),
								res.getString("TPID"));
						TCs.add(Tc);
					}
				} else {
					while (res.next()) {
						AllSelectedTC Tc = new AllSelectedTC();
						Tc.FillTestParameters(res.getString("TestID"),
								res.getString("TestCaseName"),
								res.getString("ExpectedResults"),
								res.getString("CaseDescription"),
								res.getString("Parameters"),
								res.getString("TestSteps"),
								Integer.parseInt(res.getString("RowNum")), null);
						TCs.add(Tc);
					}
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
			WriteToLogFile(e.getMessage());
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
					CloseConnection();
				} catch (SQLException e) {
					e.printStackTrace();
					WriteToLogFile(e.getMessage());
				}
			}
		}
		return TCs;
	}

	public void GetLocalIP() {
		try {
		} catch (Exception e) {
		}
	}

	public String OpenConnection() {
		try {
			String sConnection = "";

			if (DBType == 0) {
				// sConnection = "Data Source=" + DBIP +
				// ";Initial Catalog= EMSTAF;User ID=" + DBUser + ";Password=" +
				// DBPass + ";";
			} else if (DBType == 1) {
				// sConnection = "Data Source=" + DBIP +
				// "\\SQLEXPRESS;Initial Catalog=EMSTAF;User ID=" + DBUser +
				// ";Password=" + DBPass + ";";
			} else if (DBType == 2) {
				sConnection = "jdbc:jtds:sqlserver://" + DBIP + ":"
						+ "1433/EMSTAF";
			}

			myConn = java.sql.DriverManager.getConnection(sConnection,
					this.DBUser, this.DBPass);

			return "Database_Opened";
		} catch (SQLException e) {
			WriteToLogFile(e.getMessage());
			e.printStackTrace();
			return e.toString();
		} catch (Exception e) {
			WriteToLogFile(e.getMessage());
			e.printStackTrace();
			return "Database_Opened_Failed";
		}
	}

	public EMSTAFResult UpdateTable(String sql) {
		try {
			if (OpenConnection() == "Database_Opened") {
				Statement statement = myConn.createStatement();

				boolean result = statement.execute(sql);
				if (result) {
					return EMSTAFResult.SUCCESS;
				} else {
					int res = statement.getUpdateCount();
					if (res > 0) {
						return EMSTAFResult.SUCCESS;
					} else {
						return EMSTAFResult.FAILURE;
					}
				}
			} else {
				return EMSTAFResult.Database_Opened_Failed;
			}
		} catch (SQLException e) {
			WriteToLogFile(e.getMessage());
			e.printStackTrace();
			return EMSTAFResult.FAILURE;

		}
	}

	public EMSTAFResult CreateResultTable() {
		try {
			String CreateResultsTableQuery;
			String BuildNum, BuildNumber = null;

			String RCNum = null;
			String TestPlanID = null;

			String TrimDeviceMAC = DeviceMAC;
			System.out.println("Entered CreateResultTable");
			System.out.println(this.DeviceMAC);

			String query = "Select * From dbo.HostDevice Where DeviceMAC = '"
					+ this.DeviceMAC + "'";
			ResultSet Dataset;
			Statement stmt = null;
			EMSTAFResult crt = EMSTAFResult.FAILURE;

			try {
				stmt = myConn.createStatement();
				Dataset = stmt.executeQuery(query);

				while (Dataset.next()) {
					TestPlanID = Dataset.getString("TestPlanID");
					RCNum = Dataset.getString("RCNum");
					BuildNum = Dataset.getString("BuildNum");

					BuildNumber = BuildNum.replace(".", "");
					System.out.println(BuildNumber);
					break;
				}
			} catch (SQLException e) {
				WriteToLogFile(e.getMessage());
				e.printStackTrace();
			} finally {
				if (stmt != null) {
					stmt.close();
				}
			}

			query = "Select TPName From dbo.AppName Where AppNameID = "
					+ TestPlanID;
			try {
				stmt = myConn.createStatement();
				Dataset = stmt.executeQuery(query);

				while (Dataset.next()) {
					ResultsTableName = Dataset.getString("TPName");
					break;
				}
			} catch (SQLException e) {
				WriteToLogFile(e.getMessage());
				e.printStackTrace();
			} finally {
				if (stmt != null) {
					stmt.close();
				}
			}
			ResultsTableName += "_" + TrimDeviceMAC + "_" + RCNum + "_"
					+ BuildNumber;
			System.out.println(ResultsTableName);

			if (CheckTableExistence(ResultsTableName)) {
				crt = EMSTAFResult.Table_Exist;
				System.out.println("Table Exists");
				return crt;
			} else {
				CreateResultsTableQuery = "Create Table "
						+ ResultsTableName
						+ " (ID int IDENTITY(1,1) NOT FOR REPLICATION primary key, TestCaseName nvarchar(100), TestID nvarchar(100), CaseDescription nvarchar(4000),ExpectedResult varchar(8000),ActualResult varchar(8000),Result nvarchar(50),RowNum int)";
				crt = CreateTable(CreateResultsTableQuery, ResultsTableName);
				System.out.println("Table Exists");

			}

			if (crt == EMSTAFResult.Table_Created) {
				String InsQuery;
				InsQuery = "UPDATE HostDevice SET ResultsTable='"
						+ ResultsTableName + "' Where DeviceMAC = '"
						+ DeviceMAC + "'";

				if (EMSTAFResult.SUCCESS == UpdateTable(InsQuery)) {
					return EMSTAFResult.Table_Created;
				} else {
					return EMSTAFResult.Table_Creation_Failed;
				}
			} else {
				return EMSTAFResult.Table_Creation_Failed;
			}
		} catch (Exception ex) {
			System.out
					.println("SQL Exception occured at CreateResultTable():\r\n"
							+ ex.toString());
			return EMSTAFResult.Exception_Occured;
		}
	}

	public int GetExistingResultsCount() {
		try {
			if (OpenConnection() == "Database_Opened") {
				String query = "Select * From " + ResultsTableName;
				Statement statement = myConn.createStatement(
						ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY);
				ResultSet results = statement.executeQuery(query);

				if (results.last()) {
					return C_TCIndex = results.getRow();
				} else {
					return 0;
				}
			} else {
				return 0;
			}

		} catch (SQLException e2) {
			return 0;
		} catch (Exception e2) {
			return 0;
		}

	}

	public boolean CheckConnection() {
		boolean var = false;
		try {
			Class.forName("net.sourceforge.jtds.jdbc.Driver");
			if (OpenConnection() == "Database_Opened") {
				var = true;
			} else {

				var = false;
			}
		} catch (Exception ex) {
			var = false;
		}

		return var;
	}

	public EMSTAFResult CreateTable(String strSQL, String TableName) // Create
																		// Table
																		// issues
																		// to be
																		// fixed
																		// .
	{
		try {
			boolean result;
			if (OpenConnection() == "Database_Opened") {
				Statement statement = myConn.createStatement();

				result = statement.execute(strSQL);
				if (result) {
					return EMSTAFResult.Table_Created;
				} else {
					if (CheckTableExistence(TableName)) {
						return EMSTAFResult.Table_Created;
					} else {
						return EMSTAFResult.Table_Creation_Failed;
					}
				}
			} else {
				return EMSTAFResult.Table_Creation_Failed;
			}
		}

		catch (SQLException e1) {
			e1.printStackTrace();
			return EMSTAFResult.Table_Creation_Failed;
		} catch (Exception e1) {
			e1.printStackTrace();
			return EMSTAFResult.Table_Creation_Failed;
		} finally {
			CloseConnection();
			System.out.println("Exiting TableCreated method");
		}

	}

	public sqlResult CreateLogTable(String TableName) // INCOMPLETE
	{
		return sqlResult.Database_Opened;
	}

	public String UpdateTestStatus(String Status) {

		try {
			Statement statement = myConn.createStatement();
			statement.executeUpdate("INSERT INTO TestStatus (Device)VALUES('"
					+ Status + "')");

			return sqlResult.Insert_Success.toString();

		} catch (SQLException e2) {
			return sqlResult.Insert_Failed.toString();
		} catch (Exception e2) {
			return sqlResult.Insert_Failed.toString();
		}
	}

	private String InsertRecord(String strSQL) {

		try {
			if (OpenConnection() == "Database_Opened") {
				Statement statement = myConn.createStatement();
				statement.executeUpdate(strSQL);
				CloseConnection();
				return "Insert_Success";
			} else {
				return "OpenConnection failed";
			}

		} catch (SQLException e2) {
			e2.printStackTrace();
			return "Insert_Failed: " + e2.getMessage().toString();
		} catch (Exception e2) {
			e2.printStackTrace();
			return "Insert_Failed: " + e2.getMessage().toString();
		}

	}

	public String updateTime(String strSQL) {
		String iInsert;
		iInsert = strSQL;
		String i = InsertRecord(iInsert);
		if (i == "Insert_Success") {
			return "Insert_Success";
		} else {
			return i;
		}
	}

	public String InsertDeviceInfo(String DeviceIP, String DeviceMACAddress,
			String DeviceName, int Status) {
		try {
			if (OpenConnection() == "Database_Opened") {

				String sql = "DELETE FROM DeviceInfo WHERE DeviceMAC='"
						+ DeviceMACAddress + "'";
				Statement statement = myConn.createStatement();
				statement.executeUpdate(sql);

				sql = "INSERT INTO DeviceInfo(DeviceIP, DeviceMAC, DeviceName, Status) VALUES('"
						+ DeviceIP
						+ "','"
						+ DeviceMACAddress
						+ "','"
						+ DeviceName + "'," + Status + ")";
				// Statement statement = myConn.createStatement(); // Check if
				// table is updated
				statement.executeUpdate(sql);
				CloseConnection();
				return "Insert_Success";
			} else {
				return "OpenConnection Failed";
			}
		} catch (Exception t1) {
			return "Exception: " + t1.getMessage().toString();
		}
	}

	public String InsertCtrlHostInfo(String CtrlHostIP, String Port,
			String DeviceMAC) {
		try {
			if (OpenConnection() == "Database_Opened") {
				String sql;
				sql = "DELETE FROM CtrlHostInfo WHERE ControlHostIP ='"
						+ CtrlHostIP + "'";
				Statement statement = myConn.createStatement();
				statement.executeUpdate(sql);

				sql = "INSERT INTO CtrlHostInfo(ControlHostIP,Port,DeviceMAC) VALUES('"
						+ CtrlHostIP
						+ "','"
						+ Port
						+ "','"
						+ DeviceMAC
						+ "'"
						+ ")";
				// Statement statement = myConn.createStatement(); // Check if
				// table is updated
				statement.executeUpdate(sql);
				CloseConnection();
				return "Insert Success";
			} else {
			}
			return "OpenConnection Failed";
		} catch (Exception ex) {
			return "Exception: " + ex.getMessage().toString();
		}
	}

	public EMSTAFResult SetTestRunStatus(int ETR) {
		try {
			if (OpenConnection() == "Database_Opened") {
				String sqlQuery = null;
				if (ETR == 3) {
					sqlQuery = "UPDATE HostDevice SET ExitTestRun="
							+ ETR
							+ ", Status='Test Execution Temporarily Stopped From the device' WHERE DeviceMAC='"
							+ DeviceMAC + "'";
				} else if (ETR == 2) {
					sqlQuery = "UPDATE HostDevice SET ExitTestRun="
							+ ETR
							+ ", Status='Test Execution Started' WHERE DeviceMAC='"
							+ DeviceMAC + "'";
				} else if (ETR == 10) {
					sqlQuery = "UPDATE HostDevice SET ExitTestRun="
							+ 3
							+ ", Status='Connectivity lost with barcode control host PC' WHERE DeviceMAC='"
							+ DeviceMAC + "'";
				}
				Statement statement = myConn.createStatement();
				statement.executeUpdate(sqlQuery);
				CloseConnection();
				return EMSTAFResult.SUCCESS;
			} else {
				return EMSTAFResult.Database_Opened_Failed;
			}
		} catch (Exception t1) {
			t1.printStackTrace();
			return EMSTAFResult.FAILURE;
		}
	}

	public int CheckTestRunStatus() {
		try {
			int ret = 0;
			if (OpenConnection() == "Database_Opened") {
				String TestRunStatus = null;
				String sql = "SELECT ExitTestRun FROM HostDevice WHERE DeviceMAC='"
						+ DeviceMAC + "'";
				Statement statement = myConn.createStatement();
				ResultSet results = statement.executeQuery(sql);

				while (results.next()) {
					TestRunStatus = results.getString("ExitTestRun");
					ret = Integer.parseInt(TestRunStatus);
				}
				CloseConnection();
				return ret;
			} else {
				return 0;
			}
		} catch (Exception t1) {
			t1.printStackTrace();
			return 0;
		}
	}

	public int GetMUNumber() {
		String MUName = null;
		String[] SplitStr;
		int MUnumber = 0;
		try {
			if (OpenConnection() == "Database_Opened") {

				String sql = "Select MobileUnitName from HostDevice Where DeviceMAC = '"
						+ DeviceMAC + "'";
				Statement statement = myConn.createStatement();
				ResultSet results = statement.executeQuery(sql);

				while (results.next()) {
					MUName = results.getString("MobileUnitName");
					SplitStr = MUName.split("U");
					MUnumber = Integer.parseInt(SplitStr[0]);
					System.out.println(MUName);
				}
				CloseConnection();
				return MUnumber;
			} else {
				return MUnumber;
			}
		} catch (Exception t1) {
			t1.printStackTrace();
			return MUnumber;
		}

	}

	public String updateDeviceStatus(String DeviceMAC, String TestStatus) {
		try {
			if (OpenConnection() == "Database_Opened") {
				String sql = "UPDATE HostDevice SET Status='" + TestStatus
						+ "' WHERE DeviceMAC='" + DeviceMAC + "'";
				Statement statement = myConn.createStatement();
				statement.executeUpdate(sql);
				CloseConnection();
				return "Update_Success";
			} else {
				return "OpenConnection failed";
			}
		} catch (Exception t1) {
			return "Exception: " + t1.getMessage().toString();
		}
	}

	// / StopTypes are 3 and 5 and 4
	// / 5 – Indicates that user has stopped the test execution and does not
	// want to resume it later.
	// / 3 – Indicates that user has temporarily stopped the test execution and
	// wants to resume it later.
	// / 4- Indicates that Test execution has completed normally and there are
	// no more test cases to execute
	public EMSTAFResult TestRunStoppedByDUT_StatusUpdate(String DeviceMAC,
			int StopType) {
		try {
			if (OpenConnection() == "Database_Opened") {
				String sql;
				if (StopType == 5) {
					sql = "UPDATE HostDevice SET ExitTestRun=5, Status='Test Execution Permanentely Stopped From the device' WHERE DeviceMAC='"
							+ DeviceMAC + "'";
				} else if (StopType == 3) {
					sql = "UPDATE HostDevice SET ExitTestRun=3, Status='Test Execution Temporarily Stopped From the device' WHERE DeviceMAC='"
							+ DeviceMAC + "'";
				} else if (StopType == 4) {
					sql = "UPDATE HostDevice SET ExitTestRun=4, Status='Test Completed' WHERE DeviceMAC='"
							+ DeviceMAC + "'";
				} else {
					return EMSTAFResult.FAILURE;
				}

				Statement statement = myConn.createStatement();
				statement.executeUpdate(sql);
				CloseConnection();
				return EMSTAFResult.SUCCESS;
			} else {
				return EMSTAFResult.Database_Opened_Failed;
			}
		} catch (Exception t1) {
			t1.printStackTrace();
			return EMSTAFResult.FAILURE;
		}
	}

	public boolean CheckTableExistence(String TableName) {
		boolean tFound = false;

		try {
			String tabName;
			OpenConnection();
			Statement statement = myConn.createStatement();
			ResultSet results = statement
					.executeQuery("SELECT * FROM Information_Schema.Tables where Table_Type = 'BASE TABLE'");

			while (results.next()) {
				tabName = results.getString("TABLE_NAME");
				System.out.println(tabName);

				if (tabName.compareToIgnoreCase(TableName) == 0) {
					tFound = true;
				}
			}
		}

		catch (Exception e) {

		}

		CloseConnection();
		return tFound;
	}

	public String getRelevantName(String SQL, String Param) {
		ResultSet data = null;
		try {
			OpenConnection();
			Statement statement = myConn.createStatement();
			data = statement.executeQuery(SQL);
			while (data.next()) {
				return data.getString(Param);
			}
		} catch (SQLException e3) {
			e3.printStackTrace();
		} catch (Exception e3) {
			e3.printStackTrace();
		} finally {
			CloseConnection();
		}
		return null;
	}

	public String RemoveCtrlHostInfo(String DeviceMAC) {
		try {

			if (OpenConnection() == "Database_Opened") {

				String sql = "DELETE FROM CtrlHostInfo WHERE DeviceMAC='"
						+ DeviceMAC + "'";
				Statement statement = myConn.createStatement();
				statement.executeUpdate(sql);
				CloseConnection();
				return "Remove_Success";
			} else {
				return "OpenConnection falied";
			}
		} catch (Exception t1) {
			return "Exception: " + t1.getMessage().toString();
		}
	}

	public String RemoveHostDeviceInfo(String DeviceMAC) {
		try {

			if (OpenConnection() == "Database_Opened") {

				String sql = "DELETE FROM HostDevice WHERE DeviceMAC='"
						+ DeviceMAC + "'";
				Statement statement = myConn.createStatement();
				statement.executeUpdate(sql);
				CloseConnection();
				return "Remove_Success";
			} else {
				return "OpenConnection falied";
			}
		} catch (Exception t1) {
			return "Exception: " + t1.getMessage().toString();
		}
	}

	public String DeleteResultsTable() {
		try {

			if (OpenConnection() == "Database_Opened") {

				String sql = "Drop Table " + ResultsTableName;
				Statement statement = myConn.createStatement();
				statement.execute(sql);
				CloseConnection();
				return "Remove_Success";
			} else {
				return "OpenConnection falied";
			}
		} catch (Exception t1) {
			return "Exception: " + t1.getMessage().toString();
		}
	}

	public String RemoveDeviceInfo(String DeviceMAC) {
		try {

			if (OpenConnection() == "Database_Opened") {

				String sql = "DELETE FROM DeviceInfo WHERE DeviceMAC='"
						+ DeviceMAC + "'";
				Statement statement = myConn.createStatement();
				statement.executeUpdate(sql);
				CloseConnection();
				return "Remove_Success";
			} else {
				return "OpenConnection falied";
			}
		} catch (Exception t1) {
			return "Exception: " + t1.getMessage().toString();
		}
	}

	public void CleanUpDBCompletely() {
		RemoveDeviceInfo(DeviceMAC);
		RemoveHostDeviceInfo(DeviceMAC);
		RemoveCtrlHostInfo(DeviceMAC);
		DeleteResultsTable();
	}

	public String LogResults(String TableName, String TestCaseName,
			String TestID, String CaseDescription, String ExpectedResult,
			String ActualResult, String Result, int RowNo) {

		try {
			String iInsert;
			iInsert = "INSERT INTO "
					+ TableName
					+ " (TestCaseName, TestID,CaseDescription,ExpectedResult,ActualResult,Result,RowNum)VALUES('"
					+ TestCaseName + "','" + TestID + "','" + CaseDescription
					+ "','" + ExpectedResult + "','" + ActualResult + "','"
					+ Result + "'," + RowNo + ")";
			String j = InsertRecord(iInsert);
			return j.toString();
		} catch (Exception e2) {
			e2.printStackTrace();
			return "LogResults: " + e2.getMessage().toString();
		} finally {
			// CloseConnection();
		}
	}

	public String DeleteRecord(String strSQL, String dbConnection) {

		try {

			if (OpenConnection() == "Database_Opened") {
				String tDelete;
				tDelete = strSQL;
				Statement statement = myConn.createStatement();
				statement.executeUpdate(tDelete);
				CloseConnection();
				return "Delete_Success";
			} else {
				return "OpenConnection Failed";
			}

		} catch (SQLException e4) {
			return "DelException:" + e4.getMessage().toString();
		} catch (Exception e4) {
			return "DelException:" + e4.getMessage().toString();
		} finally {
			// CloseConnection();
		}
	}

	public enum sqlResult {
		Database_Opened, Database_Opened_Failed, Table_Created, Table_Exist, Table_Creation_Failed, Insert_Success, Insert_Failed, Delete_Success, Delete_Failed, Database_Closed, Database_Closed_Failed, WriteLogFile_Success, WriteLogFile_Failed

	}

	public sqlResult CloseConnection() {
		try {
			myConn.close();
			myConn = null;
			return sqlResult.Database_Closed;
		} catch (SQLException e5) {
			return sqlResult.Database_Closed_Failed;
		} catch (Exception e5) {
			return sqlResult.Database_Closed_Failed;
		}
	}

	public void WriteToLogFile(String ErrorMessage) {
		try {

			File Logfile = new File(DEST_FILE_PATH + "ScannerErrorLog.txt");
			if (!Logfile.exists()) {
				try {
					Logfile.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					WriteToLogFile(e.getMessage());
				}
			}

			BufferedWriter buf = new BufferedWriter(new FileWriter(Logfile,
					true));
			buf.append(ErrorMessage);
			buf.newLine();
			buf.flush();
			buf.close();
		} catch (java.io.IOException e) {
		}
	}

	public void ExitApplication() {
		try {
			if (myConn != null) {
				myConn.close();
				myConn = null;
			}

		} catch (Exception e) {
		}

		System.exit(0);
	}

}
