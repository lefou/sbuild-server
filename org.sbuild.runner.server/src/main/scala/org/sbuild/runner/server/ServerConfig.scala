package org.sbuild.runner.server

import de.tototec.cmdoption.CmdOption
import com.sun.org.glassfish.gmbal.Description

class ServerConfig {

  @CmdOption(names = Array("--sbuild-home"), args = Array("PATH"))
  var sbuildHome: String = _

  @CmdOption(names = Array("--host"), args = Array("HOST"))
  var host: String = "localhost"

  @CmdOption(names = Array("--port"), args = Array("PORT"))
  var port: Int = 1234

  @CmdOption(names = Array("--help", "-h"), description = "Show this help", isHelp = true)
  var help: Boolean = false

}