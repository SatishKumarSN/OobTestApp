package com.motorola.oobtestapp;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.motorola.oobtestapp.DBAccess.EMSTAFResult;

@SuppressWarnings("serial")
public class OobTestApp extends JFrame implements ActionListener {

	private final String TEST_CASE_ID = "Test_ID";
	private final String TEST_CASE_DESCRIPTION = "Test_Des";
	private final String EXPECTED_RESULT = "Exp_Result";
	private final String ACTUAL_DATA = "Actual_Data";
	private final String TEST_RESULT = "Test_Result";

	private final int MINIMUM = 0;
	private final int MAXIMUM = 100;

	private JTextField testIdField;
	private JTextField testResultField;

	private JLabel testIdLabel;
	private JLabel testCaseDescriptionLabel;
	private JLabel expectedResultLabel;
	private JLabel actualDataLabel;
	private JLabel testResultLabel;

	private JTextArea testCaseDescription;
	private JTextArea expectedResultArea;
	private JTextArea actualDataArea;

	private JScrollPane expectedScrollPane;
	private JScrollPane actualScrollPane;

	private JProgressBar progressBar;

	private JButton startBtn;
	private JButton exitBtn;

	private DBAccess dbAccess;
	String dbIP;
	String url = "jdbc:jtds:sqlserver://192.168.3.203:1433/EMSTAF";
	String id = "sa";
	String pass = "sa";
	int dbType;
	String deviceID;
	int MUNum;
	volatile List<AllSelectedTC> consolidatedTCsList;
	int TotalNoOfTestCases;
	String[] arr = new String[2];
	volatile AllSelectedTC currentTestCase;
	volatile Iterator<AllSelectedTC> TcsIter;
	volatile Socket BarcodeDispSocket;
	volatile byte[] bufSend;
	// volatile String CurrentTCResult = "FAIL";
	String CurrentTCData = null;
	volatile boolean exitAPPFromHost = false;// volatile
	private int ExitingResultsCount = 0;

	public OobTestApp() {
		JPanel panel = new JPanel();
		panel.setBackground(Color.LIGHT_GRAY);
		JFrame frame = new JFrame("OOB_Test_APP");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.getContentPane().add(panel);
		panel.setLayout(null);

		testIdLabel = new JLabel(TEST_CASE_ID);
		testIdLabel.setBounds(10, 22, 64, 14);
		panel.add(testIdLabel);

		testIdField = new JTextField(50);
		testIdField.setBounds(111, 19, 302, 20);
		testIdField.addActionListener(this);
		panel.add(testIdField);

		testCaseDescriptionLabel = new JLabel(TEST_CASE_DESCRIPTION);
		testCaseDescriptionLabel.setBounds(10, 63, 64, 14);
		panel.add(testCaseDescriptionLabel);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(111, 47, 302, 54);
		panel.add(scrollPane);

		testCaseDescription = new JTextArea(40, 100);
		scrollPane.setViewportView(testCaseDescription);
		testCaseDescription.setLineWrap(true);

		expectedResultLabel = new JLabel(EXPECTED_RESULT);
		expectedResultLabel.setBounds(10, 126, 74, 14);
		panel.add(expectedResultLabel);

		actualDataLabel = new JLabel(ACTUAL_DATA);
		actualDataLabel.setBounds(10, 195, 74, 14);
		panel.add(actualDataLabel);

		testResultLabel = new JLabel(TEST_RESULT);
		testResultLabel.setBounds(10, 243, 74, 14);
		panel.add(testResultLabel);

		expectedScrollPane = new JScrollPane();
		expectedScrollPane.setBounds(111, 112, 302, 54);
		panel.add(expectedScrollPane);

		expectedResultArea = new JTextArea(40, 100);
		expectedScrollPane.setViewportView(expectedResultArea);
		expectedResultArea.setLineWrap(true);

		actualScrollPane = new JScrollPane();
		actualScrollPane.setBounds(111, 179, 302, 50);
		panel.add(actualScrollPane);

		actualDataArea = new JTextArea(40, 100);
		actualScrollPane.setViewportView(actualDataArea);
		actualDataArea.setLineWrap(true);

		testResultField = new JTextField(50);
		testResultField.setBounds(111, 240, 302, 20);
		panel.add(testResultField);

		startBtn = new JButton("Start");
		startBtn.setBounds(124, 282, 89, 23);
		startBtn.addActionListener(this);
		panel.add(startBtn);

		exitBtn = new JButton("Exit");
		exitBtn.setBounds(313, 282, 89, 23);
		exitBtn.addActionListener(this);
		panel.add(exitBtn);

		progressBar = new JProgressBar();
		progressBar.setBounds(114, 326, 316, 14);
		progressBar.setMaximum(MAXIMUM);
		progressBar.setMinimum(MINIMUM);
		progressBar.setValue(0);
		progressBar.setStringPainted(true);
		panel.add(progressBar);

		frame.getContentPane().add(panel);

		frame.setSize(531, 437);
		frame.setVisible(true);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new OobTestApp();
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println(e.getActionCommand());

		if ("Start".equals(e.getActionCommand())) {

			if (EMSTAFResult.SUCCESS == loadTestCaseFromDB()) {
				pullandRunTests();
			}

		} else {
			showAlertDialog();
			System.exit(0);
		}
	}

	private EMSTAFResult pullandRunTests() {
		EMSTAFResult res = EMSTAFResult.FAILURE;

		System.out.println("I am in PullandRunTest worker thread...");
		TcsIter = consolidatedTCsList.iterator();
		int iter = 0;
		while (TcsIter.hasNext() && iter < ExitingResultsCount
				&& ExitingResultsCount < consolidatedTCsList.size()) {
			System.out.println("In pull and run test");
			TcsIter.next();
			iter++;
		}

		System.out.println("Size of Selected Test Cases "
				+ consolidatedTCsList.size());
		int count = 0;
		if (TcsIter.hasNext()) {
			currentTestCase = TcsIter.next();
			excuteTestCase(currentTestCase.M_Parameters);
			dbAccess.updateDeviceStatus(deviceID, currentTestCase.M_TestID);
			progressBar.setMaximum(consolidatedTCsList.size());
			testIdField.setText(currentTestCase.M_TestID);
			testCaseDescription.setText(currentTestCase.M_CaseDescription);
			expectedResultArea.setText(currentTestCase.M_ExpectedResult);
			actualDataArea.setText(currentTestCase.M_Parameters);
			progressBar.setValue(++count);
		}
		System.out.println("Coming out of PullAndRunTest code");
		return res;
	}

	private void excuteTestCase(String m_Parameters) {

	}

	private EMSTAFResult loadTestCaseFromDB() {

		if (EMSTAFResult.SUCCESS == DBInit()) {
			System.out.println("DB Init Successfull");

			if (!exitAPPFromHost) {
				dbAccess.SetTestRunStatus(2);
			} else {
				System.out.println("Test Exited from Host side");
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					dbAccess.WriteToLogFile(e.getMessage());
				}
				dbAccess.ExitApplication();
			}

			StartTest();

		} else {
			System.out.println("DB Init Failed");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				dbAccess.WriteToLogFile(e.getMessage());
				e.printStackTrace();
				dbAccess.WriteToLogFile(e.getMessage());
			}
			dbAccess.ExitApplication();
		}
		return EMSTAFResult.SUCCESS;
	}

	EMSTAFResult DBInit() {
		try {
			String line;
			int iter = 1;

			File file = new File("D:\\IP_PATH\\IPAddr.txt");

			if (file.exists()) {
				System.out.println("IP Addr File Exists");
			} else {
				System.out.println("IP Addr File Does not Exist");
			}
			FileInputStream out = new FileInputStream(file);

			BufferedReader inputBufReader = new BufferedReader(
					new InputStreamReader(out, Charset.forName("UTF-8")));
			while ((line = inputBufReader.readLine()) != null) {
				if (iter == 1)
					dbIP = line;
				if (iter == 2)
					id = line;
				if (iter == 3)
					pass = line;
				if (iter == 4)
					dbType = Integer.valueOf(line);
				iter++;
			}
			if (inputBufReader != null) {
				inputBufReader.close();
			}

			dbAccess = new DBAccess(dbIP, id, pass, dbType, getDeviceId());

			if (dbAccess.CheckConnection() == true) {
				System.out.println("Connection to DB Successfull");
			} else {
				System.out
						.println("Connect to DB failed. Application will exit now");
				dbAccess.ExitApplication();
				System.out.println("Connection to DB Un-Successfull");
			}
			EMSTAFResult res = dbAccess.CreateResultTable();
			if (EMSTAFResult.Table_Created == res) {
				return EMSTAFResult.SUCCESS;
			} else if (EMSTAFResult.Table_Exist == res) {
				ExitingResultsCount = dbAccess.GetExistingResultsCount();
				return EMSTAFResult.SUCCESS;
			} else {
				return EMSTAFResult.FAILURE;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			dbAccess.WriteToLogFile(e.getMessage());
			return EMSTAFResult.FAILURE;
		} catch (IOException e) {
			e.printStackTrace();
			dbAccess.WriteToLogFile(e.getMessage());
			return EMSTAFResult.FAILURE;
		}
	}

	public EMSTAFResult StartTest() {

		try {
			String strSQL = "SELECT * FROM HostDevice WHERE DeviceMAC='"
					+ deviceID + "'";
			String SelectedTableName = null;

			String TestPlanTableName = null;

			SelectedTableName = dbAccess.getRelevantName(strSQL, "TName");
			System.out.println("Selected Table Name " + SelectedTableName);
			strSQL = "SELECT * FROM " + SelectedTableName;

			List<AllSelectedTC> selTCs = dbAccess
					.getTCsList(strSQL, "selected");

			if (selTCs != null && selTCs.size() > 0) {
				System.out.println("Size of SelTcs " + selTCs.size());
			}

			AllSelectedTC tc = selTCs.get(0);// Use the 1st row to get the Test
												// Plan ID

			String RetrieveQuery = "Select * from Appname where AppNameID LIKE"
					+ "'" + tc.M_TPID + "'";
			TestPlanTableName = dbAccess.getRelevantName(RetrieveQuery,
					"TPName");

			RetrieveQuery = "Select * From " + TestPlanTableName;
			List<AllSelectedTC> allTCs = dbAccess.getTCsList(RetrieveQuery,
					"all");
			consolidatedTCsList = new ArrayList<AllSelectedTC>();

			Iterator<AllSelectedTC> selTcsIter = selTCs.iterator();
			Iterator<AllSelectedTC> allTcsIter = allTCs.iterator();

			while (selTcsIter.hasNext()) {
				AllSelectedTC seltc = selTcsIter.next();
				while (allTcsIter.hasNext()) {
					AllSelectedTC actual_tc = allTcsIter.next();
					if (seltc.M_TestID.equalsIgnoreCase(actual_tc.M_TestID)) {
						consolidatedTCsList.add(actual_tc);
						break;
					}
				}
			}
			TotalNoOfTestCases = consolidatedTCsList.size();
			System.out.println("Total Number of Test Cases to execute "
					+ TotalNoOfTestCases);
			return EMSTAFResult.SUCCESS;
		} catch (Exception e3) {
			System.out.println("Exception occured at StartTest()"
					+ e3.toString());
			return EMSTAFResult.Exception_Occured;
		}
	}

	private String getDeviceId() {
		try {
			Process process = Runtime.getRuntime().exec("adb devices");
			BufferedReader in = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String line = null;

			Pattern pattern = Pattern
					.compile("^([a-zA-Z0-9\\-]+)(\\s+)(device)");
			Matcher matcher;

			while ((line = in.readLine()) != null) {
				if (line.matches(pattern.pattern())) {
					matcher = pattern.matcher(line);
					if (matcher.find()) {
						System.out.println(matcher.group(1));
						deviceID = matcher.group(1);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return deviceID;
	}

	private void showAlertDialog() {
		final JOptionPane optionPane = new JOptionPane(
				"Do you want to resume later?", JOptionPane.QUESTION_MESSAGE,
				JOptionPane.YES_NO_OPTION);
		JFrame frame = new JFrame();
		final JDialog dialog = new JDialog(frame, "Alert Dialog", true);
		dialog.setContentPane(optionPane);
		dialog.setBounds(111, 240, 302, 20);
		dialog.setAlwaysOnTop(false);
		// dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		optionPane.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				String prop = e.getPropertyName();

				if (dialog.isVisible() && (e.getSource() == optionPane)
						&& (prop.equals(JOptionPane.VALUE_PROPERTY))) {
					// If you were going to check something
					// before closing the window, you'd do
					// it here.
					dialog.setVisible(false);
				}
			}
		});
		dialog.pack();
		dialog.setVisible(true);
		System.out.println(optionPane.getValue().toString());
		if (JOptionPane.UNINITIALIZED_VALUE == optionPane.getValue()) {
			System.out.println("Do Something");
		} else {
			int value = ((Integer) optionPane.getValue()).intValue();
			if (value == JOptionPane.YES_OPTION) {
				System.out
						.println("User has Selcted Resume from Test case Later.");
				performExitButtonOperation("YES");
			} else if (value == JOptionPane.NO_OPTION) {
				System.out.println("User has selected Drop Resume option");
				performExitButtonOperation("NO");
			}
		}
		return;
	}

	private void performExitButtonOperation(String choice) {
		if (choice.equals("YES")) {
			if (dbAccess != null) {
				dbAccess.SetTestRunStatus(3);
				dbAccess.ExitApplication();
			}
		} else if (choice.equals("NO")) {
			if (dbAccess != null) {
				dbAccess.updateDeviceStatus(deviceID,
						"TestExecution Permanently Stopped by User!");
				dbAccess.CleanUpDBCompletely();
				dbAccess.ExitApplication();
			}
		}
	}
}
