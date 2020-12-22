package cz.fim.uhk.smap.corona_app_client.model;

import java.util.List;

public class CoronaInformation {
    private Long id;

    private String regionCode;

    private String regionName;

    private String lastDate;

    private List<Integer> actualNumberOfCases;

    private List<Double> futureNumberOfCases;

    private int numberOfCured;

    private int numberOfDeath;

    public void setId(Long id) {
        this.id = id;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public void setLastDate(String lastDate) {
        this.lastDate = lastDate;
    }

    public void setActualNumberOfCases(List<Integer> actualNumberOfCases) {
        this.actualNumberOfCases = actualNumberOfCases;
    }

    public void setFutureNumberOfCases(List<Double> futureNumberOfCases) {
        this.futureNumberOfCases = futureNumberOfCases;
    }

    public void setNumberOfCured(int numberOfCured) {
        this.numberOfCured = numberOfCured;
    }

    public void setNumberOfDeath(int numberOfDeath) {
        this.numberOfDeath = numberOfDeath;
    }

    public Long getId() {
        return id;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public String getRegionName() {
        return regionName;
    }

    public String getLastDate() {
        return lastDate;
    }

    public List<Integer> getActualNumberOfCases() {
        return actualNumberOfCases;
    }

    public List<Double> getFutureNumberOfCases() {
        return futureNumberOfCases;
    }

    public int getNumberOfCured() {
        return numberOfCured;
    }

    public int getNumberOfDeath() {
        return numberOfDeath;
    }
}
