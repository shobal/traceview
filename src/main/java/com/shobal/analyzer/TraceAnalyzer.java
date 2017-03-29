package com.shobal.analyzer;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.NumberFormat;
import java.util.*;

import com.android.traceview.DmTraceReader;
import com.android.traceview.MethodData;
import com.android.traceview.TraceReader;

public class TraceAnalyzer {
	private ArrayList<String> mTraceFilePaths;
	static final String TRACE_SUFFIX_STRING = "trace";
	private PrintWriter mOutToJsp;
	
	public TraceAnalyzer(ArrayList<String> filePaths,PrintWriter w) {
		super();
		mTraceFilePaths = filePaths;
		mOutToJsp = w;
	}

	public void startCompareAnalyzer2(){
		if (mTraceFilePaths.size() >= 2){
			try {
				//最新版trace文件
				TraceReader reader = new DmTraceReader(mTraceFilePaths.get(0), false);
				 long totalCpuTime = reader.getTotalCpuTime();
				 long totalCpuRealTime = reader.getTotalRealTime();
				 MethodData[] methodDatas = reader.getMethods();

				//旧的trace文件
				TraceReader readerSecond = new DmTraceReader(mTraceFilePaths.get(1), false);
				 long totalCpuTimeSecond = readerSecond.getTotalCpuTime();
				 long totalCpuRealTimeSecond = readerSecond.getTotalRealTime();
				 MethodData[] methodSecondDatas = readerSecond.getMethods();
				 HashMap<Integer,MethodData> mds = new HashMap<Integer, MethodData>();
				for (MethodData md: methodSecondDatas) {
					mds.put(md.getId(),md);
				}

				//init compare info
				PerfCompareInfo.mNewTotalIncCpu = totalCpuTime;
				PerfCompareInfo.mNewTotalIncRealCpu = totalCpuRealTime;
				PerfCompareInfo.mOldTotalIncCpu = totalCpuTimeSecond;
				PerfCompareInfo.mOldTotalIncRealCpu = totalCpuRealTimeSecond;
				ArrayList<PerfCompareInfo> perfCompareInfos = new ArrayList<PerfCompareInfo>();
				for (MethodData md: methodDatas) {
					perfCompareInfos.add(new PerfCompareInfo(md,mds.remove(md.getId())));
				}

				if (mds.size() > 0){
					Iterator<Map.Entry<Integer,MethodData>> it = mds.entrySet().iterator();
					while (it.hasNext()){
						Map.Entry<Integer,MethodData> entry = it.next();
						perfCompareInfos.add(new PerfCompareInfo(null,entry.getValue()));
					}
				}

				mOutToJsp.println("<br><br>-----------------------------------Compare Result size:"+perfCompareInfos.size()+"------------------------------------");
				mOutToJsp.println("<br/><div style=\"background:#8DD7FF\">New file:"+mTraceFilePaths.get(0)+",methodDatas.size="+methodDatas.length+"</div>");
				mOutToJsp.println("<br/>totalCpuTime:"+totalCpuTime+" ,totalCpuRealTime:"+totalCpuRealTime);
				mOutToJsp.println("<br/><div style=\"background:#8DD7FF\">Old file:"+mTraceFilePaths.get(1)+",methodSecondDatas.size="+methodSecondDatas.length+"</div>");
				mOutToJsp.println("<br/>totalCpuTimeSecond:"+totalCpuTimeSecond+" ,totalCpuRealTimeSecond:"+totalCpuRealTimeSecond);
				try {
					//System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
					Collections.sort(perfCompareInfos, new Comparator<PerfCompareInfo>() {
						public int compare(PerfCompareInfo o1, PerfCompareInfo o2) {
							if (o2.mRiseCpu > o1.mRiseCpu){
								return 1;
							}else if (o1.mRiseCpu == o2.mRiseCpu){
								return 0;
							}else {
								return -1;
							}
						}
					});
				}catch (Throwable t){
					t.printStackTrace();
				}
				mOutToJsp.println("<br/><table id=\"analyze_result\"  border=\"1\">");
				mOutToJsp.println("<tr>" +
						"<td>Num</td><td>Inc Cpu Time%</td><td>Inc Cpu Time</td><td>Calls Total</td><td>Cpu Time/Call</td>" +
						"<td>Inc Real Time%</td><td>Inc Real Time</td><td>Real Time/Call</td>"+
						"<td>Excl Cpu Time%</td><td>Excl Cpu Time</td><td>Excl Real Time%</td><td>Excl Real Time</td>"+
						"<td>Cpu Rise</td><td>Name</td>"+
						"</tr>");
				NumberFormat nf = NumberFormat.getPercentInstance();
				nf.setMaximumFractionDigits(3);
				PerfCompareInfo pInfo = null;
				String trColor;
				for (int i=0;i<perfCompareInfos.size();i++) {
					pInfo = perfCompareInfos.get(i);
					MethodData md = pInfo.mNewMethodData;
					MethodData oldMd = pInfo.mOldMethodData;
					if (i % 2 == 0){
						trColor = "<tr>";
					}else{
						trColor = "<tr bgcolor=\"#8DD7FF\">";
					}
					if (md == null){
						mOutToJsp.println(trColor+"<td rowspan=\"2\">"+i+"</td><td></td><td></td><td></td><td></td>" +
								"<td></td><td></td><td></td>"+
								"<td></td><td></td><td></td><td></td>"+
								"<td rowspan=\"2\">"+nf.format(pInfo.mRiseCpu)+"</td><td>0</td>"+
								"</tr>");
					}else{
						mOutToJsp.println(trColor+
								"<td rowspan=\"2\">"+i+"</td><td>"+nf.format(pInfo.mNewIncCpuPer)+"</td><td>"+md.getElapsedInclusiveCpuTime()+"</td><td>"+md.getTotalCalls()+"</td><td>"+md.getElapsedInclusiveCpuTime()/md.getTotalCalls()+"</td>" +
								"<td>"+nf.format((float)md.getElapsedInclusiveRealTime()/totalCpuRealTime)+"</td><td>"+md.getElapsedInclusiveRealTime()+"</td><td>"+md.getElapsedInclusiveRealTime()/md.getTotalCalls()+"</td>"+
								"<td>"+nf.format((float)md.getElapsedExclusiveCpuTime()/totalCpuTime)+"</td><td>"+md.getElapsedExclusiveCpuTime()+"</td><td>"+nf.format((float)md.getElapsedExclusiveRealTime()/totalCpuRealTime)+"</td><td>"+md.getElapsedExclusiveRealTime()+"</td>"+
								"<td  rowspan=\"2\">"+nf.format(pInfo.mRiseCpu)+"</td>"+
								"<td><div style=\"width:500px;word-wrap:break-word;\" >"+md.getName().trim()+"</div></td>"+
								"</tr>");
					}

					if (oldMd != null)
						mOutToJsp.println(trColor+
								"<td>"+nf.format(pInfo.mOldIncCpuPer)+"</td><td>"+oldMd.getElapsedInclusiveCpuTime()+"</td><td>"+oldMd.getTotalCalls()+"</td><td>"+oldMd.getElapsedInclusiveCpuTime()/oldMd.getTotalCalls()+"</td>" +
								"<td>"+nf.format((float)oldMd.getElapsedInclusiveRealTime()/totalCpuRealTime)+"</td><td>"+oldMd.getElapsedInclusiveRealTime()+"</td><td>"+oldMd.getElapsedInclusiveRealTime()/oldMd.getTotalCalls()+"</td>"+
								"<td>"+nf.format((float)oldMd.getElapsedExclusiveCpuTime()/totalCpuTime)+"</td><td>"+oldMd.getElapsedExclusiveCpuTime()+"</td><td>"+nf.format((float)oldMd.getElapsedExclusiveRealTime()/totalCpuRealTime)+"</td><td>"+oldMd.getElapsedExclusiveRealTime()+"</td>"+
								"<td ><div style=\"width:500px;word-wrap:break-word;\" >"+oldMd.getName()+"</div></td>"+
								"</tr>");
					else
						mOutToJsp.println(trColor+"<td></td><td></td><td></td><td></td>" +
								"<td></td><td></td><td></td>"+
								"<td></td><td></td><td></td><td></td>"+
								"<td>0</td>"+
								"</tr>");
				}
				mOutToJsp.println("</table>");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

/*	public void startCompareAnalyzer(){
		if (mTraceFilePaths.size() >= 2){
			try {
				//最新版trace文件
				TraceReader reader = new DmTraceReader(mTraceFilePaths.get(0), false);
				final long totalCpuTime = reader.getTotalCpuTime();
				final long totalCpuRealTime = reader.getTotalRealTime();
				MethodData[] methodDatas = reader.getMethods();
				ArrayList<MethodData> methodArray = new ArrayList<MethodData>();
				Collections.addAll(methodArray,methodDatas);

				//老板trace文件
				TraceReader readerSecond = new DmTraceReader(mTraceFilePaths.get(1), false);
				final long totalCpuTimeSecond = readerSecond.getTotalCpuTime();
				final long totalCpuRealTimeSecond = readerSecond.getTotalRealTime();
				MethodData[] methodSecondDatas = readerSecond.getMethods();
				final HashMap<Integer,MethodData> mds = new HashMap<Integer, MethodData>();
				for (MethodData md: methodSecondDatas) {
					mds.put(md.getId(),md);
				}


				mOutToJsp.println("<br><br>-----------------------------------Compare Result------------------------------------");
				mOutToJsp.println("<br/>New file:"+mTraceFilePaths.get(0));
				mOutToJsp.println("<br/>totalCpuTime:"+totalCpuTime+" ,totalCpuRealTime:"+totalCpuRealTime);
				mOutToJsp.println("<br/>Old file:"+mTraceFilePaths.get(1));
				mOutToJsp.println("<br/>totalCpuTimeSecond:"+totalCpuTimeSecond+" ,totalCpuRealTimeSecond:"+totalCpuRealTimeSecond);
				try {
					System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
					Collections.sort(methodArray, new Comparator<MethodData>() {

						public int compare(MethodData o1, MethodData o2) {
							if (!mds.containsKey(o1.getId()) && !mds.containsKey(o2.getId())){
								if (o1.getElapsedInclusiveCpuTime() > o2.getElapsedInclusiveCpuTime()){
									return 1;
								}else if (o1.getElapsedInclusiveCpuTime() == o2.getElapsedInclusiveCpuTime()){
									return 0;
								}else{
									return -1;
								}
							}else if (mds.containsKey(o1.getId()) && mds.containsKey(o2.getId())){
								float oldO1CpuPer = mds.get(o1.getId()).getElapsedInclusiveCpuTime() / (float)totalCpuRealTimeSecond;
								float newO1CpuPer = o1.getElapsedInclusiveCpuTime() / (float)totalCpuTime;

								float oldO2CpuPer = mds.get(o2.getId()).getElapsedInclusiveCpuTime() / (float)totalCpuTimeSecond;
								float newO2CpuPer = o2.getElapsedInclusiveCpuTime() / (float)totalCpuTimeSecond;
								if ((newO1CpuPer -oldO1CpuPer) >= (newO2CpuPer -oldO2CpuPer)){
									return 1;
								}else if((newO1CpuPer -oldO1CpuPer) == (newO2CpuPer -oldO2CpuPer)){
									return 0;
								}else{
									return -1;
								}
							}else if (mds.containsKey(o1.getId())){
								return 1;
							}else{
								return -1;
							}
						}
					});
				}catch (Throwable t){
					t.printStackTrace();
				}
				mOutToJsp.println("<br/><table id=\"analyze_result\"  border=\"1\">");
				mOutToJsp.println("<tr>" +
						"<td>Inc Cpu Time%</td><td>Inc Cpu Time</td><td>Calls Total</td><td>Cpu Time/Call</td>" +
						"<td>Inc Real Time%</td><td>Inc Real Time</td><td>Real Time/Call</td>"+
						"<td>Excl Cpu Time%</td><td>Excl Cpu Time</td><td>Excl Real Time%</td><td>Excl Real Time</td>"+
						"<td>Cpu Rise</td><td style=\"width:200px;\">Name</td>"+
						"</tr>");
				NumberFormat nf = NumberFormat.getPercentInstance();
				nf.setMaximumFractionDigits(2);
				for (MethodData md: methodArray) {
					//mOutToJsp.println(md.getElapsedInclusiveCpuTime()+"-------"+md.getElapsedInclusiveRealTime());
					float newCpuPer = (float)md.getElapsedInclusiveCpuTime()/totalCpuTime;
					float oldCpuPer = 0;
					MethodData oldMd = null;
					if (mds.containsKey(md.getId())){
						oldMd = mds.get(md.getId());
						oldCpuPer = (float)oldMd.getElapsedInclusiveCpuTime()/totalCpuTimeSecond;
					}

					mOutToJsp.println("<tr>"+
							"<td>"+nf.format(newCpuPer)+"</td><td>"+md.getElapsedInclusiveCpuTime()+"</td><td>"+md.getTotalCalls()+"</td><td>"+md.getElapsedInclusiveCpuTime()/md.getTotalCalls()+"</td>" +
							"<td>"+nf.format((float)md.getElapsedInclusiveRealTime()/totalCpuRealTime)+"</td><td>"+md.getElapsedInclusiveRealTime()+"</td><td>"+md.getElapsedInclusiveRealTime()/md.getTotalCalls()+"</td>"+
							"<td>"+nf.format((float)md.getElapsedExclusiveCpuTime()/totalCpuTime)+"</td><td>"+md.getElapsedExclusiveCpuTime()+"</td><td>"+nf.format((float)md.getElapsedExclusiveRealTime()/totalCpuRealTime)+"</td><td>"+md.getElapsedExclusiveRealTime()+"</td>"+
							"<td>"+(newCpuPer-oldCpuPer)+"</td>"+
							"<td ><div style=\"width:500px;word-wrap:break-word;\" >"+md.getName().trim()+"</div></td>"+
							"</tr>");
					if (oldMd != null)
						mOutToJsp.println("<tr>"+
								"<td>"+nf.format(oldCpuPer)+"</td><td>"+oldMd.getElapsedInclusiveCpuTime()+"</td><td>"+oldMd.getTotalCalls()+"</td><td>"+oldMd.getElapsedInclusiveCpuTime()/oldMd.getTotalCalls()+"</td>" +
								"<td>"+nf.format((float)oldMd.getElapsedInclusiveRealTime()/totalCpuRealTime)+"</td><td>"+oldMd.getElapsedInclusiveRealTime()+"</td><td>"+oldMd.getElapsedInclusiveRealTime()/oldMd.getTotalCalls()+"</td>"+
								"<td>"+nf.format((float)oldMd.getElapsedExclusiveCpuTime()/totalCpuTime)+"</td><td>"+oldMd.getElapsedExclusiveCpuTime()+"</td><td>"+nf.format((float)oldMd.getElapsedExclusiveRealTime()/totalCpuRealTime)+"</td><td>"+oldMd.getElapsedExclusiveRealTime()+"</td>"+
								"<td></td>"+
								"<td ><div style=\"width:500px;word-wrap:break-word;\" >"+oldMd.getName()+"</div></td>"+
								"</tr>");
					else
						mOutToJsp.println("<tr><td>0</td>"+
								"</tr>");
				}
				mOutToJsp.println("</table>");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}*/

	
/*	public void startAnalyzeTraceFile(){
		for (String path : mTraceFilePaths) {
			mOutToJsp.println("-----------------------start  "+path+"-------------------------");
			try {
				TraceReader reader = new DmTraceReader(path, false);
				long totalCpuTime = reader.getTotalCpuTime();
				long totalCpuRealTime = reader.getTotalRealTime();
				long mStartTime = 0;
				long mEndTime = 0;
				Row[] threadData = reader.getThreads();
				Row mainThreadRow = null;
				for (Row row : threadData) {
					if (row.getName().contains("main")) {
						mainThreadRow = row;
						break;
					}
				}
				if (mainThreadRow == null) {
					return;
				}
				try {
					Field mGlobalStartTime =  mainThreadRow.getClass().getDeclaredField("mGlobalStartTime");
					mGlobalStartTime.setAccessible(true);
					Field mGlobalEndTime =  mainThreadRow.getClass().getDeclaredField("mGlobalEndTime");
					mGlobalEndTime.setAccessible(true);
					mStartTime = mGlobalStartTime.getLong(threadData[1]);
					mEndTime = mGlobalEndTime.getLong(threadData[1]);
				} catch (NoSuchFieldException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
				mOutToJsp.println("Methods:"+reader.getMethods().length+",ThreadLabels="+reader.getThreadLabels().size()+",Threadss="+threadData.length+","
				+reader.getTotalCpuTime()+","+reader.getTotalRealTime()+",--"+mStartTime+"---"+mEndTime);
				MethodData[] methodDatas = reader.getMethods();
				mOutToJsp.println(methodDatas[1].getId()+","+methodDatas[1].getMethodName()+"---"+methodDatas[1].getName()+"----"+methodDatas[1].getCalls()+"---"+methodDatas[1].getTopExclusiveCpuTime()+"----"+
				methodDatas[1].getTopExclusiveRealTime()+"---"+methodDatas[1].getTotalCalls()+"---"+methodDatas[1].getElapsedInclusiveCpuTime()+"---"+methodDatas[1].getElapsedInclusiveRealTime()
				+"---"+methodDatas[1].getElapsedExclusiveCpuTime()+"---"+methodDatas[1].getElapsedExclusiveRealTime()+"---");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mOutToJsp.println("-----------------------finsh analyze "+path+"-------------------------");
		}
	}*/
}
