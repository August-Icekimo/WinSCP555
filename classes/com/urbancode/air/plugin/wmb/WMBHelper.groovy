package com.urbancode.air.plugin.wmb

import com.urbancode.air.CommandHelper

public class WMBHelper {
	
	static public final String MESSAGE_FLOWS_PROP_PREFIX = 'message-flows-'
	
	static enum Version {
		VERSION6(6),
		VERSION7(7)
		
		final def version
		public Version(version) {
			this.version = version
		}
	}
	
	final def isWindows = (System.getProperty('os.name') =~ /(?i)windows/).find()
	
	final def commandPath
	final def serverName
	final def portNumber
	final def configManager
	final def brokerName
	
	final def waitTime
	final def commandHelper
	
	final def version
	
	public WMBHelper(Map args) {
		commandPath = args.commandPath
		serverName = args.serverName
		portNumber = args.portNumber
		configManager = args.configManager
		brokerName = args.brokerName
		version = args.version
		
		if (!version) {
			version = WMBHelper.Version.VERSION7
		}
		else if (version.equals('6')) {
			version = WMBHelper.Version.VERSION6
		}
		else {
			version = WMBHelper.Version.VERSION7
		}
		
		if (args.waitTime) {
			waitTime = args.waitTime
		}
		
		commandHelper = new CommandHelper(new File('.'))
	}
	
	private def buildCommand(commandPath, command) {
		def result = ''
		
		if (commandPath != null && commandPath.length() > 0) {
			result += commandPath + File.separator
		}
		result += command
		
		return result
	}
	
	private def addRemoteSettings(command) {
		
		if (serverName && portNumber && configManager) {
			command << '-i' << serverName
            //steve needed to comment out these two lines at parker
			command << '-p' << portNumber
			command << '-q' << configManager
			command << '-b' << brokerName
		}
		else {
			command << brokerName
		}
		
		return command
	}
	
	private def addWaitSettings(command) {
		
		if (waitTime) {
			command << '-w' << waitTime
		}
		
		return command
	}
	
	private def addVersionSpecificFlag(command) {
		
		switch (version) {
			case WMBHelper.Version.VERSION7:
				command << '-j'
				break
			default:
				break
		}
		
		return command
	}
	
	private def addProfileCall(command) {
		final def MQSI_PROFILE = 'mqsiprofile'
		def mqsiProfileCommand = buildCommand(commandPath, MQSI_PROFILE)

		if (isWindows) {
			throw new UnsupportedOperationException("Deployments on Windows are not supported at this time!")
		}
		
		def result = ['/bin/bash', '-c']
		result << ". ${mqsiProfileCommand}; \"\$0\" \"\$@\""
		result.addAll(command)
		
		return result
	}
	
	public def getMessageFlowNamesFromProperties(props) {
		def result = new HashSet()
		props.findAll{it.key.startsWith(WMBHelper.MESSAGE_FLOWS_PROP_PREFIX)}.each{
			result.addAll(it.value.split(',').collect{it.trim()})
		}
		result.remove('')
		
		if (!result) {
			throw new Exception("No Message Flow Names were found in Job properties.")
		}
		
		return result
	}
	
	public def getMessageFlowsFromBarFile(barFileName) {
		final def MQSI_READ_BAR = 'mqsireadbar'
		def mqsiReadBarCommand = buildCommand(commandPath, MQSI_READ_BAR)
		
		def message = "Getting Message Flows in BAR file: ${barFileName}"
		
		def command = [mqsiReadBarCommand]
		command << '-b' << barFileName
		def messageFlows = []
		
		def flowParser = { process ->
			final def DEPLOYMENT_DESC_TOKEN = '  Deployment descriptor:'
			final def MESSAGE_FLOW_PATTERN = ~/\s{2}(.*)\.cmf/
			for (line in process.getText().readLines()) {
				if (line.equals(DEPLOYMENT_DESC_TOKEN)) {
					break
				}
				else {
					def m = line =~ MESSAGE_FLOW_PATTERN
					if (m) {
						messageFlows << m.group(1)
					}
				}
			}
		}
		
		command = addProfileCall(command)
		commandHelper.runCommand(message, command, flowParser)
		
		return messageFlows
	}
	
	public def reloadBroker(executionGroup) {
		final def MQSI_RELOAD_BROKER = 'mqsireload'
		def mqsiReloadBrokerCommand = buildCommand(commandPath, MQSI_RELOAD_BROKER)
		
		def message
		
		if (executionGroup) {
			message = "Reloading Execution Group ${executionGroup} in Broker ${brokerName}"
		}
		else {
			message = "Reloading Broker ${brokerName}"
		}
		
		def command = [mqsiReloadBrokerCommand]
		command << brokerName
		
		if (executionGroup) {
			command << '-e' << executionGroup
		}
		
		command = addProfileCall(command)
		commandHelper.runCommand(message, command)
	}

	public void stopMessageFlows(executionGroup, messageFlow) {
		final def MQSI_STOP_MSG_FLOW = 'mqsistopmsgflow'
		def mqsiStopCommand = buildCommand(commandPath, MQSI_STOP_MSG_FLOW)
		
		def message
		
		if (messageFlow) {
			message = "Stopping Message Flow ${messageFlow} in Execution Group: ${executionGroup}"
		}
		else {
			message = "Stopping All Message Flows in Execution Group: ${executionGroup}"
		}
		
		
		def command = [mqsiStopCommand]
		command = addRemoteSettings(command)
		command << '-e' << executionGroup
		
		if (messageFlow) {
			def messageFlowCommand = command << '-m' << messageFlow
			messageFlowCommand = addWaitSettings(messageFlowCommand)
			messageFlowCommand = addProfileCall(messageFlowCommand)
			commandHelper.runCommand(message, messageFlowCommand)
		}
		else {
			command = addWaitSettings(command)
			addVersionSpecificFlag(command)
			command = addProfileCall(command)
			commandHelper.runCommand(message, command)
		}
	}

	public void startMessageFlows(executionGroup, messageFlow) {
		final def MQSI_START_MSG_FLOW = 'mqsistartmsgflow'
		def mqsiStartCommand = buildCommand(commandPath, MQSI_START_MSG_FLOW)
		
		def message
		if (messageFlow) {
			message = "Starting Message Flow ${messageFlow} in Execution Group: $executionGroup"
		}
		else {
			message = "Starting All Message Flows in Execution Group: $executionGroup"
		}
			
		def command = [mqsiStartCommand]
		command = addRemoteSettings(command)
		command << '-e' << executionGroup
		
		if (messageFlow) {
			def messageFlowCommand = command << '-m' << messageFlow
			messageFlowCommand = addWaitSettings(messageFlowCommand)
			messageFlowCommand = addProfileCall(messageFlowCommand)
			commandHelper.runCommand(message, messageFlowCommand)
		}
		else {
			command = addWaitSettings(command)
			addVersionSpecificFlag(command)
			command = addProfileCall(command)
			commandHelper.runCommand(message, command)
		}
	}
	
	public void deployMessageFlows(executionGroup, barFileName, fullDeploy) {
		final def MQSI_DEPLOY = 'mqsideploy'
		def mqsiDeployCommand = buildCommand(commandPath, MQSI_DEPLOY)
		
		def message = "Deploying Message Flows in BAR file: ${barFileName}"
		
		def command = [mqsiDeployCommand]
		command = addRemoteSettings(command)
		command << '-e' << executionGroup
		command << '-a' << barFileName
		
		if (fullDeploy) {
			command << '-m'
		}
		
		command	= addWaitSettings(command)
		command = addProfileCall(command)
		commandHelper.runCommand(message, command)
	}
	
	public void overrideBarProperties(barFileName, properties) {
		final def MQSI_APPLY_BAR_OVERRIDE = 'mqsiapplybaroverride'
		final def OVERRIDE_PROPS_FILE_NAME = 'override.properties'
		def mqsiApplyBarOverrideCommand = buildCommand(commandPath, MQSI_APPLY_BAR_OVERRIDE)

		def propertiesFile = new File(OVERRIDE_PROPS_FILE_NAME)
		propertiesFile.write(properties)
		
		def message = "Overriding properties in BAR file: ${barFileName}"
		
		def command = [mqsiApplyBarOverrideCommand, '-b', barFileName, '-p', propertiesFile]
		
		command = addProfileCall(command)
		commandHelper.runCommand(message, command)
		
		if (!propertiesFile.delete()) {
			throw new IllegalStateException("Unable to delete $OVERRIDE_PROPS_FILE_NAME")
		}
	}

	public void overrideBarProperties(barFileName, File propertyFile) {
		final def MQSI_APPLY_BAR_OVERRIDE = 'mqsiapplybaroverride'
		def mqsiApplyBarOverrideCommand = buildCommand(commandPath, MQSI_APPLY_BAR_OVERRIDE)

		def message = "Overriding properties in BAR file: ${barFileName}"

		def command = [mqsiApplyBarOverrideCommand, '-b', barFileName, '-p', propertyFile]

		command = addProfileCall(command)
		commandHelper.runCommand(message, command)
	}
}