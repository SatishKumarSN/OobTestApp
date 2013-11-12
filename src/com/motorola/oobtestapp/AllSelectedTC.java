package com.motorola.oobtestapp;

public class AllSelectedTC {
	public String M_TestID;
	public String M_TestCaseName;
	public String M_ExpectedResult;
	public String M_CaseDescription;
	public String M_Parameters;
	public String M_TestSteps;
	public int M_RowNum;
	public String M_TPID;

	public void FillTestParameters(String _M_TestID, String _M_TestCaseName,
			String _M_ExpectedResult, String _M_CaseDescription,
			String _M_Parameters, String _M_TestSteps, int _M_RowNum,
			String _M_TPID) {
		M_TestID = _M_TestID;
		M_TestCaseName = _M_TestCaseName;
		M_ExpectedResult = _M_ExpectedResult;
		M_CaseDescription = _M_CaseDescription;
		M_Parameters = _M_Parameters;
		M_TestSteps = _M_TestSteps;
		M_RowNum = _M_RowNum;
		M_TPID = _M_TPID;

	}
}
