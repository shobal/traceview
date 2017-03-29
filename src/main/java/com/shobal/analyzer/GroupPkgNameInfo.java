package com.shobal.analyzer;

import com.android.traceview.MethodData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/11/15.
 */
public class GroupPkgNameInfo implements Comparable{
    public long mTotalExclCpuTime;
    public String mPkgName;
    public List<MethodData> methodList = new ArrayList<MethodData>();

    public GroupPkgNameInfo() {
    }

    public GroupPkgNameInfo(String mPkgName) {
        this.mPkgName = mPkgName;
    }

    public int compareTo(Object o) {
        if (o instanceof  GroupPkgNameInfo){
            if (this.mTotalExclCpuTime > ((GroupPkgNameInfo) o).mTotalExclCpuTime){
                return -1;
            }else if (this.mTotalExclCpuTime == ((GroupPkgNameInfo) o).mTotalExclCpuTime) {
                return 0;
            } else{
                return 1;
            }
        }
        return 0;
    }
}
