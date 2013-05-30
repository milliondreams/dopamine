'use strict';

describe('Controller: CasuiCtrl', function () {

  // load the controller's module
  beforeEach(module('uiApp'));

  var CasuiCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    CasuiCtrl = $controller('CasuiCtrl', {
      $scope: scope
    });
  }));

  it('should attach a list of awesomeThings to the scope', function () {
    expect(scope.awesomeThings.length).toBe(3);
  });
});
