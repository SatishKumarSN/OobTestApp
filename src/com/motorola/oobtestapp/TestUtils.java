package com.motorola.oobtestapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TestUtils {
	private static final String SDK_PATH = "D://adt-bundle-windows-x86_64-20130917//sdk//platform-tools//adb.exe";
	public static final String CMD_FILE_PATH = "D:\\workpspace\\TestPrd32255\\TestCaseParams.txt";
	private static final String DEVICE_DESTINATION_PATH = "/data/local/tmp";
	private static String JAR_SOURCE_PATH = "D:\\JARS\\";
	public static final String SHELL = "shell";
	private static final String UIAUTOMATOR = "uiautomator";
	private static final String RUNTEST = "runtest";
	public static String TEST_CASE_ID;
	private static final String TEST_CASE = "TC";
	private static final String JAR_NAME = "JAR";
	private static final String PACKAGE_NAME = "PKG";
	private static final String HARD_RESET = "HARD_RESET";
	public static boolean mResetFlag = false;
	private static List<String> cmdStr = new ArrayList<>();

	public static void initalize() {
		cmdStr.add(SDK_PATH);
		cmdStr.add(SHELL);
		cmdStr.add(UIAUTOMATOR);
		cmdStr.add(RUNTEST);
	}

	public static String[] parseTestCaseParams(String sCurrentLine) {
		cmdStr.clear();
		initalize();
		String[] parmStr = sCurrentLine.split(":");
		for (String parm : parmStr) {
			parm = parm.trim();
			String[] tmp = parm.split(" ");
			if (tmp.length > 1 && TEST_CASE.equals(tmp[0])) {
				TEST_CASE_ID = tmp[1];
			} else if (2 == tmp.length && JAR_NAME.equals(tmp[0])) {
				cmdStr.add(tmp[1]);
				pushJartoDevice(tmp[1]);
			} else if (2 == tmp.length && PACKAGE_NAME.equals(tmp[0])) {
				cmdStr.add("-c");
				cmdStr.add(tmp[1]);
			} else if (2 == tmp.length && !mResetFlag
					&& HARD_RESET.equals(tmp[0])) {
				mResetFlag = true;
				System.out.println("Setting Reset true " + mResetFlag);
			} else if (2 == tmp.length) {
				cmdStr.add("-e");
				cmdStr.add(tmp[0]);
				cmdStr.add(tmp[1]);
			} else {
				System.out
						.println("Params format is in valid Can not Execute Test case");
				return null;
			}
		}
		String[] uiAutoRunCmd = new String[cmdStr.size()];
		for (int index = 0; index < cmdStr.size(); index++) {
			uiAutoRunCmd[index] = cmdStr.get(index);
		}
		System.out.println(cmdStr);

		return uiAutoRunCmd;
	}

	private static void pushJartoDevice(String jarName) {
		jarName = jarName.trim();
		String[] pushCmdStr = { SDK_PATH, "push", JAR_SOURCE_PATH + jarName, DEVICE_DESTINATION_PATH };
		for (String tmp : pushCmdStr) {
			System.out.println(tmp);
		}
		excuteTestCase(pushCmdStr);
	}

	public static void excuteTestCase(String[] cmdRunStr) {
		if (cmdRunStr == null) {
			System.out.println("Run time command is null");
			return;
		}
		Runtime runtime = Runtime.getRuntime();
		try {
			Process process = runtime.exec(cmdRunStr);
			readConsoleOutput(process);
		} catch (IOException e) {
			System.out.println("Io Exception while running command");
			e.printStackTrace();
		}
	}

	public static void readConsoleOutput(Process process) {
		BufferedReader input = new BufferedReader(new InputStreamReader(
				process.getInputStream()));
		String line;
		try {
			while ((line = input.readLine()) != null) {
				line = line.trim();
				if (!line.isEmpty()) {
					System.out.println(line);
				}
			}
			input.close();
		} catch (IOException e) {
			System.out.println("Io Exception while Reading output");
			e.printStackTrace();
		}
		BufferedReader error = new BufferedReader(new InputStreamReader(
				process.getErrorStream()));
		try {
			while ((line = error.readLine()) != null) {
				System.out.println(line);
				if (line.contains("error")) {
					System.out
							.println("Please check Adb Connection or Check USB debug option enabled in Settings->Developer Option ");
				}
			}
			error.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void performReboot() {
		System.out.println("Peform the Reboot on the device");
		Runtime runtime = Runtime.getRuntime();
		try {
			runtime.exec(new String[] { SDK_PATH, "reboot" });
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Waiting for the device");
		Process p;
		try {
			p = runtime.exec(new String[] { SDK_PATH, "wait-for-device" });
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
