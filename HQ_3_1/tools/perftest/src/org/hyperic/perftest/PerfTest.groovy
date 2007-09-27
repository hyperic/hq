import org.codehaus.groovy.runtime.InvokerHelper
import org.hyperic.perftest.Task
import org.hyperic.perftest.TaskExecutor

def script   = System.properties['perftest.script']
def propFile = System.properties['perftest.propfile']
def cl = new GroovyClassLoader(Thread.currentThread().contextClassLoader)

def executor = new TaskExecutor()
def binding  = new Binding()
binding.setVariable("execute", executor.&execute)
binding.setVariable('Task', Task)

new File(propFile).withInputStream { s -> 
    System.properties.load(s)
}

Class c = cl.parseClass(new File(script))
Script s = InvokerHelper.createScript(c, binding)
s.run()

executor.dumpReport()
