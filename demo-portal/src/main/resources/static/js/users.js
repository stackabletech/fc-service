$(document).ready(function() {
      console.log("Loading data");

      var partDataTable = $('#usersTable').DataTable({
            ajax: {
                url: 'users',
                dataSrc: 'items'
             },
            columns: [
                  { data: 'id' } ,
                  { data: 'participantId' },
                  { data: 'firstName' },
                  { data: 'lastName' },
                  { data: 'email' },
                  { data: 'roleIds' },
                  { data: 'username' },
                  {
                        orderable: false,
                      "render": function (data) {
                        return '<div><button type="button" class="btn btn-success" id="editButton">Edit</button></div>'
                     }
                  },
                  {
                        orderable: false,
                      "render": function (data) {
                        return '<div><button type="button" class="btn btn-danger" id="deleteButton">Delete</button></div>'
                        }
                  }

                   ]

            } );

            $('#usersTable').on('click', '#editButton',function(e){
                 e.preventDefault();
                 var data = partDataTable.row( $(this).parents('tr')).data();
                //console.log("edit call"+JSON.stringify(data));
                $("#editData").val(JSON.stringify(data,null, ' '));
//                $("#editData").val(JSON.stringify(data));
                $('#editModal').modal('show');
            });

            $('#usersTable').on('click', '#deleteButton',function(e){
                 e.preventDefault();

                  var check = confirm("Are you sure you want to delete?");
                     if (check == true) {
                         console.log("Confirm delete");
                         var data = partDataTable.row( $(this).parents('tr')).data();
                         //console.log("delete call"+JSON.stringify(data));

                             $.ajax('/users/'+data.id, {
                                type: 'DELETE',  // http method
                                contentType: "application/json;charset=utf-8",
                                success: function (data, status, xhr) {
                                console.log("post delete success");
                                partDataTable.ajax.reload();

                                },
                                error: function (jqXhr, textStatus, errorMessage) {
                                console.log("post delete failure");

                                }
                            });
                        } else {
                             console.log("Cancel delete");
                              return ;
                    }
             });

            $('#submitEditButton').on('click', function(e){

                 var normalData=$("#editData").val();
                 var dataObj=JSON.parse(normalData);
                 console.log("data::"+dataObj['id']);
                     $.ajax('/users/'+dataObj['id'], {
                            type: 'PUT',  // http method
                            contentType: "application/json",
                            data: JSON.stringify(dataObj,'\n',''),  // data to submit
                            success: function (data, status, xhr) {
                             console.log("post edit success");
                              $('#editModal').modal('hide');
                              partDataTable.ajax.reload();
                            },
                            error: function (jqXhr, textStatus, errorMessage) {
                             console.log("post edit failure");
                            }
                     });
             });

            $('#submitAddButton').on('click', function(e){

                 var normalData=$("#addData").val();
                 var data=JSON.parse(normalData);
                 //console.log("data"+data);

                     $.ajax('/users', {
                            type: 'POST',  // http method
                            contentType: "application/json;charset=utf-8",
                            data: JSON.stringify(data),  // data to submit
                            success: function (data, status, xhr) {
                             console.log("post edit success");
                              $('#addModal').modal('hide');
                              partDataTable.ajax.reload();
                            },
                            error: function (jqXhr, textStatus, errorMessage) {
                             console.log("post edit failure");
                            }
                     });
             });

            $('#addButton').on('click', function(e){
                e.preventDefault();
                $('#addModal').modal('show');
             });

            $('#addButton').on('click', function(e){
                 e.preventDefault();
                console.log("Add call");
            });

            $('.close').on('click', function(e){
                $('#editModal').modal('hide');
                $('#addModal').modal('hide');
            });

 });