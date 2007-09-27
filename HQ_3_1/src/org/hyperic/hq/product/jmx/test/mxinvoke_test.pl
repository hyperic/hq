#generate an MBean with as many method signature combinations as possible and script to test.
#usage:
#perl mxinvoke_test.pl > mxinvoke.sh
#javac MxTestServer*.java
#java -Dcom.sun.management.jmxremote -cp . MxTestServer
#sh mxinvoke.sh
#go get a coffee (or a beer) while it pounds away
#XXX cmdline PluginDumper invocation will melt host machine.
use strict;

my(@types) = qw{
    String
    Short
    Integer
    Long
    Double
    Boolean
};

my(%native_types) = (
    short => 'S',
    int => 'I',
    long => 'J',
    double => 'D',
    boolean => 'Z'
);

my $string = 'aaaa';
my $short  = 0;
my $int = 0;
my $long = 0;
my $double = 0.0;

my(%defaults) = (
    String  => sub { $string++; },
    Short   => sub { $short++ },
    Integer => sub { $int++ },
    Int     => sub { $int++ },
    Long    => sub { $long++ },
    Double  => sub { $double++ },
    Boolean => sub { 'true' },
);
for (keys %defaults) {
    $defaults{lc $_} = $defaults{$_};
}

push @types, sort keys %native_types;

my $value_body = "return value;";

my $array_body =
    qq{if (values.length == 0) throw new IllegalArgumentException("length==0");
        else return values[0];};

my $list_body =
    qq{if (values.size() == 0) throw new IllegalArgumentException("size==0");
        else return values.get(0);};

my(@interface, @methods, @invoke);

for my $type (@types) {
    my $method = "invoke_$type";

    gen_invoke($method, $type);
    gen_method("$type $method($type value)",
               $value_body);

    gen_invoke($method, [$type]);
    gen_method("$type $method($type [] values)", $array_body);

    unless ($native_types{$type}) {
        gen_invoke($method, new List $type);
        gen_method("Object $method(List values)", $list_body);
    }

    for my $arg_type (@types) {

        gen_invoke($method, $arg_type, $type);
        gen_method("$type $method($arg_type arg_type, $type value)",
                   $value_body);

        gen_invoke($method, $arg_type, [$type]);
        gen_method("$type $method($arg_type arg_type, $type [] values)",
                   $array_body);

        gen_invoke($method, [$type], $arg_type);
        gen_method("$type $method($type [] values, $arg_type arg_type)",
                   $array_body);

        unless ($native_types{$type}) {
            gen_invoke($method, $arg_type, new List $type);
            gen_method("Object $method($arg_type arg_type, List values)",
                       $list_body);

            gen_invoke($method, new List $type, $arg_type);
            gen_method("Object $method(List values, $arg_type arg_type)",
                       $list_body);
        }
    }
}

gen_attribute('int', 'MethodCount', scalar @methods);

my $imports = <<EOF;
import java.util.List;

EOF

open FH, ">MxTestServer.java" or die $!;
print FH "import javax.management.*;\n";
print FH $imports;
print FH "public class MxTestServer implements MxTestServerMBean {\n\n";
print FH join "\n", @methods;
print FH <<EOF;

    public static void main(String[] args) throws Exception {
        ObjectName name = new ObjectName("hyperic:Server=MxTest");
        MBeanServer server =
            (MBeanServer)MBeanServerFactory.findMBeanServer(null).get(0);
        server.registerMBean(new MxTestServer(), name);
        System.in.read();
    }
EOF
print FH "\n}\n";
close FH;

open FH, ">MxTestServerMBean.java" or die $!;
print FH $imports;
print FH "public interface MxTestServerMBean {\n\n";
print FH join "    \n", @interface;
print FH "\n}\n";
close FH;

my(@cmd) = (
    'java',
    '-Dcontrol.action.validate=false',
    '-jar', 'pdk/lib/hq-product.jar',
    #'-Dlog=debug',
    '-Dplugins.include=jmx',
    "'-Djmx.url=ptql:State.Name.eq=java,Args.*.ct=MxTestServer,Pid.Pid.ne=\$\$'",
    '-t', '"Sun JVM 1.5"', '-m', 'control'
    );

for my $args (@invoke) {
    my $method = shift @$args;
    print qq{@cmd -a "hyperic:Server=MxTest::$method" "-Dcontrol.args=@$args"\n};
}

sub gen_method {
    my($sig, $body) = @_;
    $sig = "public $sig";
    push @interface, "$sig;";
    push @methods, <<EOF;
    $sig {
        $body
    }
EOF
}

sub gen_attribute {
    my($type, $attr, $value) = @_;
    my $sig = "public $type get$attr()";
    push @interface, "$sig;";
    push @methods, <<EOF;
    $sig {
        return $value;
    }
EOF
}

sub gen_invoke {
    my $method = shift;
    my(@args) = ($method);
    my(@sig, @params);

    for my $type (@_) {
        my $sig_type;
        my $num = 1;
        my $is_array = ref($type) eq 'ARRAY';
        my $is_list = ref($type) eq 'List';

        if ($is_array or $is_list) {
            $type = $type->[0];
            $num = 3;

            if ($is_array) {
                $sig_type = '[' . ($native_types{$type} || "Ljava.lang.$type;");
            }
            else {
                $sig_type = 'java.util.List';
            }
        }
        else {
            $sig_type = $native_types{$type} ? $type : "java.lang.$type";
        }

        push @sig, $sig_type;
        push @params, map { $defaults{$type}->() } 1..$num;
    }

    push @args, '@(' . join(';', @sig) . ')';
    push @args, @params;

    push @invoke, \@args;
}

{
    package List;
    sub new {
        bless [$_[1]];
    }
}
