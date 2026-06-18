package com.drake.droidblox.service.root

import java.io.BufferedReader
import java.io.InputStreamReader

class RootManager {

    fun isRootAvailable(): Boolean = checkRootAccess()

    fun checkRootAccess(): Boolean {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo root_ok"))
            val out = BufferedReader(InputStreamReader(proc.inputStream)).readLine()
            proc.waitFor()
            out == "root_ok"
        } catch (_: Exception) { false }
    }

    fun applyFFlags(fflagsJson: String): Boolean {
        return try {
            val dir = "/data/user/0/com.roblox.client/files/exe/ClientAppSettings"
            val script = buildString {
                appendLine("mkdir -p $dir")
                appendLine("chmod 755 $dir")
                appendLine("cat > ${dir}/ClientAppSettings.json << 'ENDOFFLAGS'")
                appendLine(fflagsJson)
                appendLine("ENDOFFLAGS")
                appendLine("chmod 644 ${dir}/ClientAppSettings.json")
            }
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", script))
            proc.waitFor() == 0
        } catch (_: Exception) { false }
    }

    fun execSu(command: String): ShellResult {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val out = BufferedReader(InputStreamReader(proc.inputStream)).readText()
            val err = BufferedReader(InputStreamReader(proc.errorStream)).readText()
            ShellResult(proc.waitFor() == 0, out, err)
        } catch (e: Exception) {
            ShellResult(false, "", e.message ?: "")
        }
    }

    fun execSh(command: String): ShellResult {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val out = BufferedReader(InputStreamReader(proc.inputStream)).readText()
            val err = BufferedReader(InputStreamReader(proc.errorStream)).readText()
            ShellResult(proc.waitFor() == 0, out, err)
        } catch (e: Exception) {
            ShellResult(false, "", e.message ?: "")
        }
    }

    fun getRobloxPid(): Int? {
        val result = execSu("pidof com.roblox.client")
        return if (result.success && result.stdout.isNotBlank())
            result.stdout.trim().split(" ").first().toIntOrNull()
        else null
    }

    data class ShellResult(val success: Boolean, val stdout: String, val stderr: String)
}
