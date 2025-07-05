package com.gec.marine.entity;

import lombok.Data;

@Data
public class Result<T>
{

    // 声明常量，表示请求处理成功这个状态
    public static final String SUCCESS = "SUCCESS";

    // 声明常量，表示请求处理失败这个状态
    public static final String FAILED = "FAILED";

    // 请求处理的结果，是成功还是失败
    private String processResult;
    // 查询结果
    private T queryResultData;
    // 请求处理失败时，错误消息
    private String errorMessage;
    /**
     * 工具方法：处理请求成功，没有查询结果需要返回
     * @return
     */
    public static Result ok() {

        Result result = new Result();
        result.setProcessResult(SUCCESS);
        return result;
    }

    /**
     * 工具方法：处理请求成功，并且有查询结果需要封装
     * @param queryResultData
     * @return
     * @param <T>
     */
    public static <T> Result<T> ok(T queryResultData) {

        Result result = new Result();

        result.setProcessResult(SUCCESS);
        result.setQueryResultData(queryResultData);

        return result;
    }

    /**
     * 工具方法：处理请求失败
     * @param errorMessage
     * @return
     */
    public static Result failed(String errorMessage) {

        Result result = new Result();
        result.setProcessResult(FAILED);
        result.setErrorMessage(errorMessage);

        return result;
    }

    public String getProcessResult() {
        return processResult;
    }

    public void setProcessResult(String processResult) {
        this.processResult = processResult;
    }

    public T getQueryResultData() {
        return queryResultData;
    }

    public void setQueryResultData(T queryResultData) {
        this.queryResultData = queryResultData;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "Result{" +
                "processResult='" + processResult + '\'' +
                ", queryResultData=" + queryResultData +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }

    public Result(String processResult, T queryResultData, String errorMessage) {
        this.processResult = processResult;
        this.queryResultData = queryResultData;
        this.errorMessage = errorMessage;
    }

    public Result() {
    }


}
