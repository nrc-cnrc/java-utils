use strict;

my $file = $ARGV[0];

print STDERR "Fixing badly escaped quotes in file $file\n";

open FILE, "<$file" || die "Could not open file $file for input";
my $lineNum = 1;

while( my $line = <FILE>)  {
    $line =~ s/\\""([^,])/""$1/g;
    print $line;
}

1;
