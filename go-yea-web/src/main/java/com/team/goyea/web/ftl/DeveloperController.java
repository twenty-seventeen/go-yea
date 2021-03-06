package com.team.goyea.web.ftl;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import com.yea.core.remote.client.ClientRegister;
import com.yea.core.remote.promise.Promise;
import com.yea.core.remote.struct.CallAct;
import com.yea.remote.netty.client.NettyClient;
import com.yea.web.jsonbody.JsonPropFilter;

@Controller
public class DeveloperController {
	
	@RequestMapping("/developer/generator/load.html")
    public String loadGenerator(ModelMap model, @RequestParam Map<String, String> config) throws Throwable {
        return "developer/generator";
    }
	@RequestMapping("/developer/generator/generator.html")
    public String generator(ModelMap model, @RequestParam Map<String, String> config) throws Throwable {
        String message = null;
		if (StringUtils.isEmpty(config.get("moduleName"))) {
			message = "请输入模块名";
		} else if (StringUtils.isEmpty(config.get("basePackagePath"))) {
			message = "请输入根包路径";
		} else if (StringUtils.isEmpty(config.get("tableName"))) {
			message = "请输入表名";
		} else if (!StringUtils.isEmpty(config.get("daoGenerateable")) && config.get("daoGenerateable").trim().equals("false")) {
			if (StringUtils.isEmpty(config.get("commonDaoPackagePath"))) {
				message = "请输入公共DAO包路径名";
			} else if (StringUtils.isEmpty(config.get("commonDaoName"))) {
				message = "请输入公共DAO名";
			}
		}

		if (message == null) {
			CallAct act = new CallAct();
			act.setActName("generatorAct");
			Promise<Set<String>> promise = ClientRegister.<Set<String>>getInstance().send(act, config);
			Set<String> tablenames = promise.awaitObject(10000);
			model.put("tablenames", tablenames);
			message = "生成成功";
		}
        
		model.put("message", message);
		
        return "developer/generatorresult";
    }
	
	@RequestMapping("/nettynode/statistics")
	@JsonPropFilter(type = NodeStatistics.class)
    public NodeStatistics nodeStatistics() throws Throwable {
		WebApplicationContext webApplicationContext = ContextLoader.getCurrentWebApplicationContext();
		NettyClient nettyClient = (NettyClient) webApplicationContext.getBean(NettyClient.class);
		NodeStatistics nodeStatistics = new NodeStatistics();
		nodeStatistics.setStatistics(nettyClient.useStatistics());
		return nodeStatistics;
	}
	
	private class NodeStatistics {
		private String statistics;

		@SuppressWarnings("unused")
		public String getStatistics() {
			return statistics;
		}

		public void setStatistics(String statistics) {
			this.statistics = statistics;
		}
	}
}
