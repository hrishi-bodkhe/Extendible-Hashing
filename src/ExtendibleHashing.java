import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

class Employee {
    int id;
    int amount;
    String name;
    int item;
    Random rand = new Random();

    public Employee() {
        this.id = rand.nextInt(ExtendibleHashing.Records);
        this.amount = rand.nextInt(500001);
        this.name = nameGenerator();
        this.item = rand.nextInt(1501);
    }

    public String nameGenerator() {
        String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        Random rand = new Random();
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < 3; i++)
            name.append(alpha.charAt((rand.nextInt(alpha.length()))));

        return name.toString();
    }

    public String getBits() {                            //returns the id as bits
        StringBuilder ans = new StringBuilder(Integer.toBinaryString(this.id));
        while (ans.length() < ExtendibleHashing.MAXBITS) {
            ans.reverse();
            ans.append("0");
            ans.reverse();
        }

        return ans.toString();
    }

    @Override                                           //prints the employee structure
    public String toString() {
        return this.id + " " + this.amount + " " + this.name + " " + this.item;
    }
}

class Bucket extends Employee {
    int bindex;
    int freeSpaces;
    int localDepth;
    int nextBucket;
    Employee[] rec = new Employee[ExtendibleHashing.B];     //Records of a Bucket

    Bucket() {
    }

    public Bucket(Bucket b) {
        this.freeSpaces = b.freeSpaces;
        this.localDepth = b.localDepth;
        this.nextBucket = b.nextBucket;
        this.bindex = b.bindex;
        this.rec = b.rec;
    }

    public void setter(int i) {
        this.freeSpaces = ExtendibleHashing.B;
        this.localDepth = 0;
        this.nextBucket = -1;
        this.bindex = i;
    }

    public String display() {             //prints records of a bucket
        StringBuilder str = new StringBuilder();
        int fspace = ExtendibleHashing.B - this.freeSpaces;
        for (Employee employee : rec) {
            if (employee != null && fspace > 0) {
                str.append(employee.toString()).append(", ");
                --fspace;
            }
        }
        return str.toString();
    }
}

public class ExtendibleHashing {
    public static final int B = 2;                   //bucket size
    public static final int Records = 16;            //total records
    public static final int SSM = 10000;            //defining secondary memory size
    public static int globalDepth = 0;
    public static int MAXBITS = 16;//Integer.toBinaryString(Records).length();
    public static int mindex = 0;                                 //mindex is memory index
    static Employee[] data;                         //storing the initial Records
    static Bucket[] memory;                         //for storing buckets
    static LinkedHashMap<String, Bucket> map;             //Bucket Address Table
    public static int expandFlag = 0;
    public static int splitFlag = 0;

    static {
        data = new Employee[Records];
        map = new LinkedHashMap<>();
        memory = new Bucket[SSM];
    }

    public static void main(String[] args) throws IOException {
        FileWriter writer = new FileWriter("output.txt");
        writer.flush();
        writer.close();
        //Generating Records
        generateRecords();
        createBuckets();                              //creating buckets in memory


        Bucket b = memory[mindex];
        b.setter(mindex);
        mindex++;
        map.put("", b);                   //adding first empty bucket
        int dindex = 0;                    //dindex points to data records
        int rindex = 0;                    //rindex points to bucket records
        while (b.freeSpaces > 0) {
            b.rec[rindex++] = data[dindex++];
            --b.freeSpaces;
        }

        tableExtension();
        expandFlag = 1;

        while (dindex < Records) {
            if (insert(dindex)) {
                ++dindex;
            }
        }

        displayMap(map);
        System.out.println(globalDepth);
    }


    static boolean insert(int dindex) {
        Employee e = data[dindex];
        String key = e.getBits().substring(0, globalDepth);
        Bucket b = map.get(key);
        Bucket prev = b;
        Bucket prev2 = b;

        while (b.nextBucket != -1) {
            if (b.freeSpaces > 0) {
                b.rec[B - b.freeSpaces] = e;
                --b.freeSpaces;
                expandFlag = 0;
                splitFlag = 0;
                return true;
            } else {
                prev = b;
                b = memory[b.nextBucket];
            }
        }

        if (b.freeSpaces > 0) {
            b.rec[B - b.freeSpaces] = e;
            --b.freeSpaces;
            expandFlag = 0;
            splitFlag = 0;
            return true;
        } else {
            if (globalDepth > b.localDepth) {
                int prevLocalDepth = b.localDepth;              //saving previous localDepth
                if (splitFlag == 1) {
                    Bucket newb = memory[mindex];
                    newb.setter(mindex);
                    ++mindex;
                    newb.localDepth = prevLocalDepth;
                    newb.nextBucket = -1;
                    b.nextBucket = newb.bindex;
                    splitFlag = 0;
                    return false;
                }
                splitFlag = 0;
                ++prev2.localDepth;     //increase local depth of old bucket
                rehash(prev2, prevLocalDepth);
            } else {
                if (expandFlag == 1) {
                    Bucket newb = memory[mindex];
                    newb.setter(mindex);
                    ++mindex;
                    newb.localDepth = prev2.localDepth;
                    newb.nextBucket = -1;
                    b.nextBucket = newb.bindex;
                    expandFlag = 0;
                    return false;
                }
                tableExtension();
                ++globalDepth;
            }
        }
        return false;
    }

    static void rehash(Bucket b, int prevLocalDepth) {
        Employee[] rec = new Employee[1000];
        int len = 0;
        int i = 0;
        Bucket tempb = b;
        while (tempb.nextBucket != -1) {
            for (int j = 0; j < B - tempb.freeSpaces; ++j) {
                rec[i++] = tempb.rec[j];
                ++len;
            }
            tempb = memory[tempb.nextBucket];
        }
        for (int j = 0; j < B - tempb.freeSpaces; ++j) {
            rec[i++] = tempb.rec[j];
            ++len;
        }

        b.freeSpaces = B;
        b.nextBucket = -1;

        int counter = (int) Math.pow(2, globalDepth - prevLocalDepth) / 2;

        Bucket newb = memory[mindex];                 //create new bucket
        newb.setter(mindex);
        ++mindex;
        newb.localDepth = b.localDepth;                 //new bucket local depth = old bucket local depth

        for (Map.Entry e : map.entrySet()) {
            if (counter > 0) {                            //point the half keys to new bucket
                if (e.getValue() == b) {
                    e.setValue(newb);
                    --counter;
                }
            } else
                break;
        }

        splitFlag = 1;

        //rehash stored records
        for (int j = 0; j < len; ++j) {
            Employee e = rec[j];
            String key = e.getBits().substring(0, globalDepth);
            //System.out.println(key);
            Bucket buk = map.get(key);

            if (buk.freeSpaces > 0) {
                buk.rec[B - buk.freeSpaces] = e;
                --buk.freeSpaces;
            } else {
                boolean record_was_inserted = false;
                while (buk.nextBucket != -1) {               //searching the chain
                    buk = memory[buk.nextBucket];
                    if (buk.freeSpaces > 0) {
                        buk.rec[B - buk.freeSpaces] = e;
                        --buk.freeSpaces;
                        record_was_inserted = true;
                    }
                }

                if (!record_was_inserted && buk.freeSpaces == 0) {
                    Bucket newb1 = memory[mindex];      //adding new bucket to the chain
                    newb1.setter(mindex);
                    newb1.nextBucket = -1;
                    newb1.localDepth = buk.localDepth;
                    buk.nextBucket = mindex;
                    ++mindex;
                    newb1.rec[B - newb1.freeSpaces] = e;
                    --newb1.freeSpaces;
                }
            }
        }
    }

    static void tableExtension() {
        LinkedHashMap<String, Bucket> temp = new LinkedHashMap<>();
        if (globalDepth == 0) {
            temp.put("0", map.get(""));
            temp.put("1", map.get(""));
            ++globalDepth;
        } else {
            for (String key : map.keySet()) {
                temp.put(key + "0", map.get(key));
                temp.put(key + "1", map.get(key));
            }
        }

        map.clear();
        map.putAll(temp);
        temp.clear();
        expandFlag = 1;
//        return map;
    }

    static void createBuckets() {
        int i = 0;
        while (i < SSM) {
            Bucket b = new Bucket();
            b.setter(i);
            memory[i++] = b;
        }
    }

    static void displayMap(LinkedHashMap<String, Bucket> map) throws IOException {
        StringBuilder str = new StringBuilder();
        for (Map.Entry<String, Bucket> e : map.entrySet()) {
            str.append(e.getKey()).append(" = {");
            Bucket b = e.getValue();
            if (b != null) {
                while (b.nextBucket != -1) {
                    if (b != null) {
                        str.append("Bucket: ").append(b.display()).append("->");
                    }
                    b = memory[b.nextBucket];
                }
                if (b != null) {
                    str.append("Bucket: ").append(b.display());
                }
            }
            str.append("\t}\n");
        }

        FileWriter writer = new FileWriter("output.txt", true);
        writer.append(str.toString());
        writer.flush();
        writer.close();
    }

    static void displayData(Employee[] data) {          //displaying initial records
        for (Employee e : data)
            System.out.println(e.toString());
    }

    static void generateRecords() {          //generating and storing initial records
//        int[] arr = {16, 14, 4, 3, 19, 21, 10, 2, 11, 20, 6, 26, 9, 17, 25, 15, 13, 7, 22, 24, 5, 18, 12, 23, 27, 1, 8};
//        int[] arr = {1, 1, 1, 1, 1, 1, 1, 8, 8, 8, 8};
//        int[] arr = {13, 10, 6, 5, 15, 2, 3, 7, 8, 1, 12, 11, 4, 9, 14};
//        for (int i = 0; i < arr.length; i++) {
//            data[i] = new Employee();
//            data[i].id = arr[i];
//        }
        int i = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader("input.txt"));
            String line = null;

            while ((line = br.readLine()) != null) {
                String[] temp = line.split(" ");
                data[i] = new Employee();
                data[i].id = Integer.parseInt(temp[0]);
                data[i].amount = Integer.parseInt(temp[1]);
                data[i].name = temp[2];
                data[i].item = Integer.parseInt(temp[3]);
                ++i;
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
