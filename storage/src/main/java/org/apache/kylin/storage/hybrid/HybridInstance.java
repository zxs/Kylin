package org.apache.kylin.storage.hybrid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.kylin.common.KylinConfig;
import org.apache.kylin.common.persistence.RootPersistentEntity;
import org.apache.kylin.metadata.model.MeasureDesc;
import org.apache.kylin.metadata.model.TblColRef;
import org.apache.kylin.metadata.project.RealizationEntry;
import org.apache.kylin.metadata.realization.IRealization;
import org.apache.kylin.metadata.realization.RealizationRegistry;
import org.apache.kylin.metadata.realization.RealizationType;
import org.apache.kylin.metadata.realization.SQLDigest;

import java.util.List;

/**
 * Created by shaoshi on 2/13/15.
 */

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class HybridInstance extends RootPersistentEntity implements IRealization {

    @JsonIgnore
    private KylinConfig config;

    @JsonProperty("name")
    private String name;

    @JsonProperty("historyRealization")
    private RealizationEntry historyRealization;

    @JsonProperty("realTimeRealization")
    private RealizationEntry realTimeRealization;

    private IRealization historyRealizationInstance;
    private IRealization realTimeRealizationInstance;
    private String projectName;

    public void init() {
        RealizationRegistry registry = RealizationRegistry.getInstance(config);
        historyRealizationInstance = registry.getRealization(historyRealization.getType(), historyRealization.getRealization());
        realTimeRealizationInstance = registry.getRealization(realTimeRealization.getType(), realTimeRealization.getRealization());

    }

    @Override
    public boolean isCapable(SQLDigest digest) {
        return historyRealizationInstance.isCapable(digest) || realTimeRealizationInstance.isCapable(digest);
    }

    @Override
    public int getCost(SQLDigest digest) {
        int cost = Math.min(historyRealizationInstance.getCost(digest), realTimeRealizationInstance.getCost(digest)) - 1;

        return cost < 0 ? 0 : cost;
    }

    @Override
    public RealizationType getType() {
        return RealizationType.HYBRID;
    }

    @Override
    public String getFactTable() {
        return null;
    }

    @Override
    public List<TblColRef> getAllColumns() {
        return null;
    }

    @Override
    public List<MeasureDesc> getMeasures() {
        return null;
    }

    @Override
    public boolean isReady() {
        return historyRealizationInstance.isReady() || realTimeRealizationInstance.isReady();
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    @Override
    public String getCanonicalName() {
        return getType() + "[name=" + name + "]";
    }

    @Override
    public String getProjectName() {
        return projectName;
    }

    @Override
    public void setProjectName(String prjName) {
        projectName = prjName;
    }

    public KylinConfig getConfig() {
        return config;
    }

    public void setConfig(KylinConfig config) {
        this.config = config;
    }

    public RealizationEntry getHistoryRealization() {
        return historyRealization;
    }

    public RealizationEntry getRealTimeRealization() {
        return realTimeRealization;
    }

    public IRealization getHistoryRealizationInstance() {
        return historyRealizationInstance;
    }

    public IRealization getRealTimeRealizationInstance() {
        return realTimeRealizationInstance;
    }

    @Override
    public long getDateRangeStart() {
        return historyRealizationInstance.getDateRangeStart();
    }

    @Override
    public long getDateRangeEnd() {
        return realTimeRealizationInstance.getDateRangeEnd();
    }
}
