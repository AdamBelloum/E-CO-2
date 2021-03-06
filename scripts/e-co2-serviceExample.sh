#!/bin/bash


# 
#    1. An educator wants to create a course description and wants to see:
#          How dose the job market and education align 
#    2. An educator has created a course description and wants to see how it compares with:
#         The average job market
#         The average of the offered courses
#         Top 3 most similar course
#    3. A practitioner has her/his CV and wants to compere it with:
#         The average job market
#         Top 3 most similar job posts 

HOST=localhost
PORT=9999

function json2CSV {
 jq -r '[.["action_plan_strategies_dsrm04"],.["analysis_tools_for_decision_making_dseng03"],.["analytic_support_to_other_organisation_dsdk04"],.["analytics_for_decision_making_dsda03"],.["big_data_analytics_platform_dsda05"],.["business_process_dsdk01"],.["complex_data_visulization_dsda06"],.["computational_science_dseng02"],.["contribution_to_research_objectives_dsrm05"],.["customer_data_analysis_dsdk05"],.["data_blending_dsda04"],.["data_collection_and_integration_dsdm03"],.["data_curation_dsdm05"],.["data_management_plan_dsdm01"],.["data_models_dsdm02"],.["devise_new_applications_dsrm03"],.["engineering_principles_dseng01"],.["improve_existing_services_dsdk02"],.["innovative_ideas_development_dsrm06"],.["ipr_and_ethical_issues_dsdm06"],.["marketing_data_analysis_dsdk06"],.["new_data_analytics_applications_dseng06"],.["participation_financial_decisions_dsdk03"],.["predictive_analytics_dsda01"],.["relational_and_non-relational_databases_dseng04"],.["repository_of_analysis_history_dsdm04"],.["scientific_method_dsrm01"],.["security_service_management_dseng05"],.["statistical_techniques_dsda02"],.["systematic_study_dsrm02"]] | @csv' $1
}


function writeHeader {
 echo "field,dsrm04,dseng03,dsdk04,dsda03,dsda05,dsdk01,dsda06,dseng02,dsrm05,dsdk05,dsda04,dsdm03,dsdm05,dsdm01,dsdm02,dsrm03,dseng01,dsdk02,dsrm06,dsdm06,dsdk06,dseng06,dsdk03,dsda01,dseng04,dsdm04,dsrm01,dseng05,dsda02,dsrm02" > $1
}

function scenario1 { 
#  Get education average 
 curl -X GET http://$HOST:$PORT/e-co2/average/course -o courseAvg.json
#  Build csv header
 writeHeader  avg.csv
#  Convert JSON to CSV

 education=`json2CSV courseAvg.json`
 echo "education,$education" >> avg.csv

#  Get job market average 
 curl -X GET http://$HOST:$PORT/e-co2/average/job -o jobAvg.json
 
 #  Convert JSON to CSV
 jobMarket=`json2CSV jobAvg.json`
 echo "jobMarket,$jobMarket" >> avg.csv
}
 

 function scenario2 { 
# Classify course
 jsonFile=$1
 cID=`curl -H "Content-Type: application/json" -X POST -d @$jsonFile http://$HOST:$PORT/e-co2/classification/course`
 
 
 
  Get job market average 
 curl -X GET http://$HOST:$PORT/e-co2/average/job -o jobAvg.json 
 #  Get education average 
 curl -X GET http://$HOST:$PORT/e-co2/average/course -o courseAvg.json
 
 
#  wait for classification to end
  resp=`curl -X GET http://$HOST:$PORT/e-co2/classification/$cID`
  while [ $resp == "202" ]           
  do
    echo $cID not done 
    sleep 20
    resp=`curl -X GET http://$HOST:$PORT/e-co2/classification/$cID`
  done    
  
#   Get the classification
  curl -X GET http://$HOST:$PORT/e-co2/classification/$cID -o classification.json
  
#   Now we can ask for profiling. Returns an ordered list of the most similar courses it also includes a "rank" field
  pID=`curl -X GET http://$HOST:$PORT/e-co2/profile/courses/$cID`
 
 #  wait for profiling to end
  resp=`curl -X GET http://$HOST:$PORT/e-co2/profile/$pID`
  while [ $resp == "202" ]           
  do
    echo $pID not done 
    sleep 20
    resp=`curl -X GET http://$HOST:$PORT/e-co2/profile/$pID`
  done   
  
  curl -X GET http://$HOST:$PORT/e-co2/profile/$pID -o profile.json
  
  
  writeHeader  course1.csv 
  
  education=`json2CSV courseAvg.json`
  echo "education,$education" >> course1.csv
 
  jobMarket=`json2CSV jobAvg.json`
  echo "jobMarket,$jobMarket" >> course1.csv
 
 
  uid=`jq 'keys' classification.json  | jq .[0] | sed "s/^\([\"']\)\(.*\)\1\$/\2/g"`
  jq .$uid classification.json > tmp.json
  profile=`json2CSV tmp.json`
  
  echo "$uid,$profile" >> course1.csv
  
  writeHeader  course2.csv
  
  echo "$uid,$profile" >> course2.csv
  
  
  jq .[1] profile.json > tmp.json
  match1=`json2CSV tmp.json`
  id=`jq .[1].jobid profile.json`
  echo "$id,$match1" >> course2.csv
   
   
  jq .[2] profile.json > tmp.json
  match2=`json2CSV tmp.json`
  id=`jq .[2].jobid profile.json`
  echo "$id,$match2" >> course2.csv
  
  
  
  jq .[3] profile.json > tmp.json
  match3=`json2CSV tmp.json`
  id=`jq .[3].jobid profile.json`
  echo "$id,$match3" >> course2.csv
  
  
  jq .[4] profile.json > tmp.json
  match4=`json2CSV tmp.json`
  
  id=`jq .[4].jobid profile.json`
  echo "$id,$match4" >> course2.csv
  
    
}




 function scenario3  { 
# # Classify cv
jsonFile=$1

 cID=`curl -H "Content-Type: application/json" -X POST -d @$jsonFile http://$HOST:$PORT/e-co2/classification/cv`
 
 
 #  Get job market average 
 curl -X GET http://$HOST:$PORT/e-co2/average/job -o jobAvg.json 
 
#  wait for classification to end
  resp=`curl -X GET http://$HOST:$PORT/e-co2/classification/$cID`
  while [ $resp == "202" ]           
  do
    echo $cID not done 
    sleep 20
    resp=`curl -X GET http://$HOST:$PORT/e-co2/classification/$cID`
  done    
  
#   Get the classification
  curl -X GET http://$HOST:$PORT/e-co2/classification/$cID -o classification.json
  
#   Now we can ask for profiling. Returns an ordered list of the most similar jobs it also includes a "rank" field
  pID=`curl -X GET http://$HOST:$PORT/e-co2/profile/jobs/$cID`
 
 #  wait for profiling to end
  resp=`curl -X GET http://$HOST:$PORT/e-co2/profile/$pID`
  while [ $resp == "202" ]           
  do
    echo $pID not done 
    sleep 20
    resp=`curl -X GET http://$HOST:$PORT/e-co2/profile/$pID`
  done   
  
  curl -X GET http://$HOST:$PORT/e-co2/profile/$pID -o profile.json
  
  
  writeHeader  cv1.csv 
  
  
  jobMarket=`json2CSV jobAvg.json`
  echo "jobMarket,$jobMarket" >> cv1.csv  
  
  uid=`jq 'keys' classification.json  | jq .[0] | sed "s/^\([\"']\)\(.*\)\1\$/\2/g"`
  jq .$uid classification.json > tmp.json
  
  cv=`json2CSV tmp.json`
  echo "$uid,$cv" >> cv1.csv
  
  
  writeHeader cv2.csv
  
  echo "$uid,$cv" >> cv2.csv
  jq .[1] profile.json > tmp.json
  match1=`json2CSV tmp.json`
  
  id=`jq .[1].jobid profile.json`
  echo "$id,$match1" >> cv2.csv
  echo "$id,$match1" >> cv1.csv
  
 
  jq .[2] profile.json > tmp.json
  match2=`json2CSV tmp.json`
  id=`jq .[2].jobid profile.json`
  echo "$id,$match2" >> cv2.csv
  echo "$id,$match2" >> cv1.csv
  
  jq .[3] profile.json > tmp.json
  match3=`json2CSV tmp.json`
  id=`jq .[3].jobid profile.json`
  echo "$id,$match3" >> cv2.csv
  echo "$id,$match3" >> cv2.csv
   
}
   
function profileCource {
 url=$1
 curl -s \" $url \" &> response.html 
 host=`echo $url | awk -F/ '{print $3}' | sed 's/\./_/g'`
 
 id=$timestamp"_"$host
    
    
 w3m response.html -dump -T text/html > course"_"$id.txt
 
 content=`tr '\n' ' ' < course"_"$id.txt | sed 's/<[^>]*>//g'`
 
 echo "{\"id\":\"course"_"$id\",\"title\":\"Sample Data Science Cource\",\"contents\":\"$content\"}" > course"_"$id.json
 
 scenario2 course"_"$id.json

}



# scenario1

# echo "{\"id\":\"course1\",\"title\":\"Data Analytics MSc\",\"contents\":\"Core courses Preliminary Mathematics for Statisticians 1 Probability 2 Statistical Inference2 Regression Models2 Introduction to R Programming Data Management and Analytics using SAS Bayesian Statistics Generalised Linear Models Big Data Analytics One Course is optional for students with sufficient background in Linear Algebra and Calculus. Two students who have already completed an equivalent course can substitute this course by any other optional course, including optional courses offered as part of the MRes in Advanced Statistics (see the website for details). In your project (60 credits) you will model data collected from research in environmental science, assessed by a dissertation. Optional courses Choose two courses from group 1, one course from group 2 with the remaining courses coming from groups 1, 2 or 3. Group 1 Programming Artificial Intelligence Information Retrieval Machine Learning Group 2 Professional Skills Data Analysis Group 3 Biostatistics Multivariate Methods Time Series Design of Experiments Stochastic Processes Environmental Statistics Financial Statistics Statistical Genetics Spatial Statistics Functional Data Analysis In your project (60 credits) you will tackle a complex data analytical problem or develop novel approaches to solving data analytical challenges. The Data Lab We work closely with The Data Lab, an internationally leading research and innovation centre in data science.\"}" > cource_uid_3.json
# scenario2 cource_uid_3.json



# echo "{\"id\":\"cv1\",\"title\":\"Sample Resume Data Scientist\",\"contents\":\"Professional Profile A former Ruby and Java programmer with newly acquired skills, an insatiable intellectual curiosity, and the ability to mine hidden gems located within large sets of structured, semi-structured and unstructured data. Able to leverage a heavy dose of mathematics and applied statistics with visualization and a healthy sense of exploration. Education Grad Certificate, Data Mining 2012University of California, San Diego Relevant Courses: Data Mining Methods and Techniques, Data Preparation for Data Mining and Advanced Methods and Applications B.S. Computer Science 2009San Francisco State University Core Competencies Strategic Thinking: Able to influence the strategic direction of the company by identifying opportunities in large, rich data sets and creating and implementing data driven strategies that fuel growth including revenue and profits. Modeling: Design and implement statistical / predictive models and cutting edge algorithms utilizing diverse sources of data to predict demand, risk and price elasticity. Experience with creating ETL processes to source and link data. Analytics: Utilize analytical applications like SAS to identify trends and relationships between different pieces of data, draw appropriate conclusions and translate analytical findings into risk management and marketing strategies that drive value. Drive Enhancements: Develop tools and reports that help users access and analyze data resulting in higher revenues and margins and a better customer experience. Communications and Project Management: Capable of turning dry analysis into an exciting story that influences the direction of the business and communicating with diverse teams to take a project from start to finish. Collaborate with product teams to develop and support our internal data platform and to support ongoing analyses. Skills and Tools NoSQL data stores (Cassandra, MongoDB) Hadoop, MySQL, Big Table, MapReduce, SAS Large-scale, distributed systems design and development Scaling, performance and scheduling and ETL techniques C, C++, Java, Ruby on Rails Experience Cool Jeans San Francisco, CA Data Analyst2012-present Work closely with various teams across the company to identify and solve business challenges utilizing large structured, semi-structured, and unstructured data in a distributed processing environment. Develop a new pricing strategy for Total Jeans that boosted margins by 2 percent and analyzed customer buying habits which correctly predicated the resurgence of dark blue denim giving us a jump on the competition. Analyze large datasets to provide strategic direction to the company. Perform quantitative analysis of product sales trends to recommend pricing decisions. Conduct cost and benefit analysis on new ideas. Scrutinize and track customer behavior to identify trends and unmet needs. Develop statistical models to forecast inventory and procurement cycles. Assist in developing internal tools for data analysis. Programmer 2010-2011 Coded, tested, debugged, implemented and documented apps using Java and Ruby. Developed eCommerce solutions and social networking functionality. Designed, developed and maintained eCommerce and social networking applications. Built report interfaces and data feeds. Gathered and collected information from various programs, analyzed time requirements and prepared documentation to change existing programs. Professional Affiliations and Education NoSQL and Big Data Conference 2013 Hadoop Hackathon: Learn Map Reduce 2013 Member, Silicon Valley Big Data Meetup\"}" > cv.json
# scenario3 cv.json

profileCource "https://www.uva.nl/onderwijs/master/masteropleidingen/masteropleidingen/masteropleidingen/content/folder-5/information-studies-data-science/study-programme/study-programme.html#anker-programme-outline-"
