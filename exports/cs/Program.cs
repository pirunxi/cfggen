using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using cfg;

namespace test
{
    class Program
    {
        static void Main(string[] args)
        {
            CfgMgr.DataDir.Dir = "e:/cfggen.git/trunk/data";
            CfgMgr.DataDir.Encoding = "utf-8";
            System.Console.WriteLine("++++");
            CfgMgr.Load();
            System.Console.WriteLine("++++");
        }
    }
}
