# bank-transfer-api

In order to execute the project:
```
./bin/sbt run
```

## Api Model:

Create Customer:
```
curl --request POST \
  --url http://localhost:8080/v1/customers \
  --header 'content-type: application/json' \
  --data '{
	"email": "bob@example.com"
}'
```

Get Customer
```
curl --request GET \
  --url http://localhost:8080/v1/customers/${CUSTOMER_ID}
```

Create Account:
```
curl --request POST \
  --url http://localhost:8080/v1/customers/${CUSTOMER_ID}/accounts \
  --header 'content-type: application/json'
  --data '{
  	"currency": "EUR"
  }'
```

Get Account:
```
curl --request GET \
  --url http://localhost:8080/v1/accounts/${ACCOUNT_ID} \
  --header 'content-type: application/json'
```

Create Deposit:
```
curl --request POST \
  --url http://localhost:8080/v1/accounts/${ACCOUNT_ID}/deposits \
  --header 'content-type: application/json' \
  --data '{
	"total": "42.0000",
	"currency": "EUR"
}'
```

Create Withdrawal:
```
curl --request POST \
  --url http://localhost:8080/v1/accounts/${ACCOUNT_ID}/withdrawals \
  --header 'content-type: application/json' \
  --data '{
	"total": "10",
	"currency": "EUR"
}'
```

Create Transfer:
```
curl --request POST \
  --url http://localhost:8080/v1/accounts/${ACCOUNT_ID}/transfers \
  --header 'content-type: application/json' \
  --data '{
	"target_id": "337bcc6a-9aa6-11e8-aae2-0575fd2b3cfb",
	"money": {
		"total": "21",
		"currency": "EUR"
	}
}'
```

Get operation:
```
curl --request GET \
  --url http://localhost:8080/v1/operations/${OPERATION_ID}
```

## Technologies
 - Scala
 - Akka HTTP
 - sbt