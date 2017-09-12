using System;
using System.Collections.Generic;
using System.IO;

namespace cfg.marshal
{
    public interface IMarshaller
    {
        void Write(DataWriter io);
    }


    public class DataWriter
    {
        public enum Mode
        {
            ForCfgGen,
            ForDataStream,
        }

        private Mode mode;
        private readonly List<string> _lines = new List<string>();

        public DataWriter(Mode mode)
        {
            this.mode = mode;
        }

        public bool IsDataStreamMode()
        {
            return mode == Mode.ForDataStream;
        }

        private void Add(string s)
        {
            _lines.Add(s);
        }

        public void Write(bool x)
        {
            Add(x ? "true" : "false");
        }

        public void Write(int x)
        {
            Add(x.ToString());
        }

        public void Write(long x)
        {
            Add(x.ToString());
        }

        public void Write(float x)
        {
            Add(x.ToString());
        }

        public void Write(string x)
        {
            if(IsDataStreamMode())
                Add(x);
            else
                Add(string.IsNullOrEmpty(x) ? "null" : x.Replace("#", "%#").Replace("]", "%]").Replace("null", "%null"));
        }

        public void Write(IMarshaller x, bool writename)
        {
            if(writename)
                Write(x.GetType().Name);
            x.Write(this);
        }


        public void Write(object x)
        {
            if (x is bool)
            {
               Write((bool)x); 
            }
            else if (x is int)
            {
                Write((int)x);
            }
            else if (x is long)
            {
                Write((long)x);
            }
            else if (x is float)
            {
                Write((float)x);
            }
            else if (x is string)
            {
                Write((string)x);
            }
            else if (x is IMarshaller)
            {
                Write((IMarshaller) x, false);
            }
            else
            {
                throw new Exception("unknown marshal type;" + x.GetType());
            }
        }

        public void WriteSize(int x)
        {
            Write(x);
        }

        public void WriteEnd()
        {
            Add("]]");
        }

        public void Write<T>(List<T> x, bool writename)
        {
            if(IsDataStreamMode())
                WriteSize(x.Count);
            foreach(var v in x)
            {
                if(writename)
                    Write(v.GetType().Name);
                Write(v);
            }
            if(!IsDataStreamMode())
                WriteEnd();
        }

        public void Write<T>(HashSet<T> x, bool writename)
        {
            if (IsDataStreamMode())
                WriteSize(x.Count);
            foreach (var v in x)
            {
                if (writename)
                    Write(v.GetType().Name);
                Write(v);
            }
            if (!IsDataStreamMode())
                WriteEnd();
        }

        public void Write<K, V>(Dictionary<K, V> x, bool writekeyname, bool writevaluename)
        {
            if (IsDataStreamMode())
                WriteSize(x.Count);
            foreach (var v in x)
            {
                if(writekeyname)
                    Write(v.Key.GetType().Name);
                Write(v.Key);
                if(writevaluename)
                    Write(v.Value.GetType().Name);
                Write(v.Value);
            }
            if (!IsDataStreamMode())
                WriteEnd();
        }

        public void Write<K, V>(Dictionary<K, V> x)
        {
            Write(x, false, false);
        }

        public void Save(string output)
        {
            File.WriteAllLines(output, _lines.ToArray());
        }
    }
}
